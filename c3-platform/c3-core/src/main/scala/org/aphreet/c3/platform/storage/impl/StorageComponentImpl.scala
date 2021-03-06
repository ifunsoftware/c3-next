/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.aphreet.c3.platform.storage.impl

import collection.mutable
import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Path => NioPath, FileVisitResult, SimpleFileVisitor, Files}
import javax.annotation.PreDestroy
import org.aphreet.c3.platform.common.{Logger, SimpleCloseableIterable, Path, Constants}
import org.aphreet.c3.platform.config.PlatformConfigComponent
import org.aphreet.c3.platform.exception.{ConfigurationException, StorageException, StorageNotFoundException}
import org.aphreet.c3.platform.resource.{ResourceAddress, IdGenerator, Resource}
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.storage.dispatcher.StorageDispatcherComponent
import org.aphreet.c3.platform.task.{TaskComponent, Task, IterableTask}

trait StorageComponentImpl extends StorageComponent{

  this: PlatformConfigComponent
    with StorageDispatcherComponent
    with TaskComponent =>

  val storageManager: StorageManager = new StorageManagerImpl(new StorageConfigAccessorImpl(configPersister))

  class StorageManagerImpl(val configAccessor: StorageConfigAccessor) extends StorageManager with ConflictResolverProvider {

    val log = Logger(classOf[StorageComponentImpl])

    private val storages = new mutable.HashMap[String, Storage]

    private val factories = new mutable.HashMap[String, StorageFactory]

    val indexConfigAccessor: StorageIndexConfigAccessor = configAccessor.indexConfigAccessor

    lazy val systemId = getSystemId

    lazy val storageLocation = defaultStoragePath

    val conflictResolvers = new mutable.HashMap[String, ConflictResolver]()

    {
      log info "Starting StorageManager..."
      updateDispatcher()

      taskManager.submitTask(new CapacityMonitoringTask(this))
    }

    @PreDestroy
    def destroy() {
      log info "Stopping StorageManager..."
    }

    def registerFactory(factory: StorageFactory) {
      factories.synchronized {
        factories.put(factory.name, factory)
      }

      createExistentStoragesForFactory(factory)
    }

    def unregisterFactory(factory: StorageFactory) {

      storages.synchronized {
        factory.storages.foreach(s => unregisterStorage(s))
      }

      factories.synchronized {
        factories - factory.name
      }

      updateDispatcher()

    }

    def storageForId(id: String): Option[Storage] = {
      storages.get(id)
    }

    def storageForResource(resource: Resource): Option[StorageLike] = {
      storageForAddress(ResourceAddress(resource.address))
    }

    def storageForAddress(address: ResourceAddress): Option[StorageLike] = {
      storageDispatcher.selectStorageForAddress(address).flatMap(params => storageForId(params.id))
    }

    def createStorage(storageType: String, storagePath: Option[Path]): Storage = {
      val storage = factories.get(storageType) match {
        case Some(factory) => {

          var stId = ""

          do {
            stId = IdGenerator.generateStorageId
          } while (!isIdCorrect(stId))

          log info "Creating new storage with id: " + stId

          factory.createStorage(new StorageParams(stId, storagePath.getOrElse(defaultStoragePath), factory.name,
            RW(Constants.STORAGE_MODE_NONE),
            indexConfigAccessor.load,
            new mutable.HashMap[String, String]),
            systemId, this)
        }
        case None => throw new StorageException("Can't find factory for type: " + storageType)
      }

      registerStorage(storage)
      addStorageToParams(storage)

      storage
    }


    def listStorages: List[Storage] =
      storages.map(_._2).toList.distinct

    def removeStorage(storage: Storage) {

      if (storage.count == 0
        || storage.mode == U(Constants.STORAGE_MODE_MIGRATION)) {

        for ((id, st) <- storages if st eq storage) {
          storages - id
        }

        factories.values.foreach(_.storages - storage)

        removeStorageFromParams(storage)

        storage.close()

        removeStorageData(storage)

        log info "Storage with id " + storage.id + " removed"
      } else {
        throw new StorageException("Failed to remove non-empty storage")
      }
    }


    def listStorageTypes: List[String] =
      factories.map(e => e._2.name).toList

    def setStorageMode(id: String, mode: StorageMode) {
      storages.get(id) match {
        case Some(s) => {
          s.mode = mode
          updateStorageParams(s)
        }
        case None => throw new StorageNotFoundException(id)
      }
    }

    def updateStorageParams(storage: Storage) {

      val oldParams = configAccessor.load

      configAccessor.store(storage.params :: oldParams.filter(_.id != storage.id))

      storages.put(storage.id, storage)

      updateDispatcher()
    }

    def createIndex(index: StorageIndex) {
      indexConfigAccessor.update(list => index :: list)

      taskManager.submitTask(new CreateIndexTask(listStorages, index))
    }

    def removeIndex(name: String) {

      indexConfigAccessor.load.find(_.name == name) match {
        case Some(index) => {

          indexConfigAccessor.update(list => list.filter(i => i.name != name))
          storages.values.foreach(_.removeIndex(index))

        }
        case None => throw new StorageException("Index with name " + name + " not found")
      }
    }

    def mergeStorages(fromId: String, toId: String) {
      mergeStorages(fromId, toId)
    }

    def resetStorages() {

      log.info("Performing storage reset, clearing ALL existing data")

      log.info("Closing all storages")
      val storageList = storages.values.toList

      for (storage <- storageList) {
        unregisterStorage(storage)
        storage.close()
      }

      log.info("Removing all storages from factroies")
      factories.values.foreach(_.storages.clear())

      log.info("Removing all storage data")
      for (storage <- storageList) {
        removeStorageData(storage)
      }

      log.info("Recreating storages from configuration")
      factories.values.foreach(createExistentStoragesForFactory)

      log.info("Reseting storage dispatcher")
      storageDispatcher.resetDispatcher(configAccessor.load)
    }


    def conflictResolverFor(resource: Resource) = {
      val contentType = resource.contentType
      conflictResolvers.get(contentType) match {
        case Some(resolver) => resolver
        case None => new DefaultConflictResolver
      }
    }

    def registerConflictResolver(contentType: String, conflictResolver: ConflictResolver) {
      conflictResolvers.synchronized(
        conflictResolvers.put(contentType, conflictResolver)
      )
    }

    private def registerStorage(storage: Storage) {
      storages.put(storage.id, storage)
    }

    private def unregisterStorage(storage: Storage) {
      storages.remove(storage.id)
    }

    private def updateDispatcher() {
      storageDispatcher.setStorageParams(configAccessor.load)
    }

    private def isIdCorrect(newId: String): Boolean = {

      log info "Checking id '" + newId + "' for existence in platform params"

      val storageParams = configAccessor.load

      !storageParams.exists(param => param.containsId(newId))
    }

    private def removeStorageFromParams(storage: Storage) {
      configAccessor.update(storageParams => storageParams.filter(_.id != storage.id))

      updateDispatcher()
    }

    private def addStorageToParams(storage: Storage) {
      configAccessor.update(storageParams => storage.params :: storageParams)
      updateDispatcher()
    }

    private def createExistentStoragesForFactory(factory: StorageFactory) {
      val storageParams = configAccessor.load

      log info "Existent storages: " + storageParams

      log info "Looking for existent storages for factory: " + factory.name

      for (param <- storageParams) {

        if (param.storageType.equals(factory.name)) {
          log info "Restoring existent storage: " + param.toString
          registerStorage(factory.createStorage(param, systemId, this))
        }
      }
    }

    private def removeStorageData(storage: Storage) {

      log.info("Going to remove storage data dir " + storage.fullPath)

      Files.walkFileTree(storage.fullPath.file.toPath, new SimpleFileVisitor[NioPath] {
        override def visitFile(file: NioPath, attrs: BasicFileAttributes): FileVisitResult = {
          log.info("Deleting path " + file)
          Files.delete(file)
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: NioPath, e: IOException): FileVisitResult = {
          log.info("Deleting directory " + dir)

          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      })
    }

    private def getSystemId: String = {
      platformConfigManager.getPlatformProperties.get(Constants.C3_SYSTEM_ID) match {
        case Some(value) => value
        case None => throw new ConfigurationException("Failed to get systemId from params")
      }
    }

    private def defaultStoragePath: Path = {
      Path(platformConfigManager.dataDir.getAbsolutePath).append("storages")
    }

    class CreateIndexTask(storages: List[Storage], index: StorageIndex) extends IterableTask(new SimpleCloseableIterable(storages)) {
      def processElement(element: Storage) {
        log.info("Creating index for storage: " + element.id)
        element.createIndex(index)
        log.info("Index for storage " + element.id + " has been created")
      }
    }

    class CapacityMonitoringTask(storageManager: StorageManager) extends Task {
      override def step() {
        for (storage <- storageManager.listStorages) {
          if (storage.availableCapacity < 100L * 1024 * 1024) {
            if (storage.mode.allowWrite) {
              storage.mode = RO(Constants.STORAGE_MODE_CAPACITY)
            }
          } else if (!storage.mode.allowWrite) {
            if (storage.availableCapacity > 500L * 1024 * 1024) {
              if (storage.mode == RO(Constants.STORAGE_MODE_CAPACITY)) {
                storage.mode = RW(Constants.STORAGE_MODE_CAPACITY)
              }
            }
          }
        }
        Thread.sleep(10 * 1000)
      }
    }
  }
}
