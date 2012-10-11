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

import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.storage._

import dispatcher.StorageDispatcher

import org.aphreet.c3.platform.common.{Path, Constants}
import org.aphreet.c3.platform.storage.volume.VolumeManager

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import java.io.{IOException, File}
import org.aphreet.c3.platform.config.PlatformConfigManager
import org.aphreet.c3.platform.exception.{ConfigurationException, StorageException, StorageNotFoundException}
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.resource.{ResourceAddress, IdGenerator, Resource}
import collection.mutable
import java.nio.file.{Path => NioPath, FileVisitResult, SimpleFileVisitor, Files}
import java.nio.file.attribute.BasicFileAttributes

@Component("storageManager")
class StorageManagerImpl extends StorageManager{

  val log = LogFactory.getLog(getClass)

  private val storages = new mutable.HashMap[String, Storage]

  private val factories = new mutable.HashMap[String, StorageFactory]

  @Autowired
  var storageDispatcher:StorageDispatcher = null

  @Autowired
  var configAccessor : StorageConfigAccessor = null

  @Autowired
  var volumeManager : VolumeManager = null

  @Autowired
  var platformConfigManager:PlatformConfigManager = null

  lazy val systemId = getSystemId

  @PostConstruct
  def init(){
    log info "Starting StorageManager..."
    updateDispatcher()
  }

  @PreDestroy
  def destroy(){
    log info "Stopping StorageManager..."
  }

  def registerFactory(factory:StorageFactory) {
    factories.synchronized{
      factories.put(factory.name, factory)
    }

    createExistentStoragesForFactory(factory)
  }

  def unregisterFactory(factory:StorageFactory) {

    storages.synchronized{
      factory.storages.foreach(s => unregisterStorage(s))
    }

    factories.synchronized{
      factories - factory.name
    }

    updateDispatcher()

  }

  def storageForId(id:String):Storage = {
    storages.get(id) match {
      case Some(storage) => storage
      case None => throw new StorageNotFoundException(id)
    }
  }

  def storageForResource(resource:Resource):Storage = {
    storageForAddress(ResourceAddress(resource.address))
  }

  def storageForAddress(address:ResourceAddress):Storage = {
    storageDispatcher.selectStorageForAddress(address) match {
      case Some(params) => storageForId(params.id)
      case None => throw new StorageNotFoundException("Can't find storage for resource " + address.stringValue)
    }
  }

  def createStorage(storageType:String, storagePath:Path){
    val storage = factories.get(storageType) match {
      case Some(factory) => {

        var stId = ""

        do{
          stId = IdGenerator.generateStorageId
        }while(!isIdCorrect(stId))

        log info "Creating new storage with id: " + stId

        factory.createStorage(new StorageParams(stId, storagePath, factory.name,
                                                  RW(Constants.STORAGE_MODE_NONE),
                                                  List(), new mutable.HashMap[String, String]),
                                               systemId)
      }
      case None => throw new StorageException("Can't find factory for type: " + storageType)
    }

    registerStorage(storage)
    addStorageToParams(storage)
  }


  def listStorages:List[Storage] =
    storages.map(_._2).toList.distinct

  def removeStorage(storage:Storage) {

    if(storage.count == 0
            || storage.mode == U(Constants.STORAGE_MODE_MIGRATION)){

      for((id, st) <- storages if st eq storage) {
        storages - id
      }

      factories.values.foreach(_.storages - storage)

      removeStorageFromParams(storage)

      storage.close()

      removeStorageData(storage)

      log info "Storage with id " + storage.id + " removed"
    }else{
      throw new StorageException("Failed to remove non-empty storage")
    }
  }


  def listStorageTypes:List[String] =
    factories.map(e => e._2.name).toList

  def setStorageMode(id:String, mode:StorageMode) {
    storages.get(id) match {
      case Some(s) => {
        s.mode = mode
        updateStorageParams(s)
      }
      case None => throw new StorageNotFoundException(id)
    }
  }

  def updateStorageParams(storage:Storage) {

    val oldParams = configAccessor.load

    configAccessor.store(storage.params :: oldParams.filter(_.id != storage.id))

    storages.put(storage.id, storage)

    updateDispatcher()
  }

  def createIndex(id:String, index:StorageIndex) {
    val storage = storageForId(id)

    if(storage.count != 0){
      throw new StorageException("Unable to create index on storage with content")
    }

    storage.createIndex(index)

    updateStorageParams(storage)
  }

  def removeIndex(id:String, name:String) {
    val storage = storageForId(id)

    val indexes = storage.params.indexes.filter(_.name != name)

    if(indexes.size > 0){
      storage.removeIndex(indexes.head)
      updateStorageParams(storage)
    }else{
      throw new StorageException("Index not found")
    }
  }

  def mergeStorages(fromId:String, toId:String){
    storageDispatcher.mergeStorages(fromId, toId)
  }

  def resetStorages(){

    log.info("Performing storage reset, clearing ALL existing data")

    log.info("Closing all storages")
    val storageList = storages.values.toList

    for(storage <- storageList){
      unregisterStorage(storage)
      storage.close()
    }

    log.info("Removing all storages from factroies")
    factories.values.foreach(_.storages.clear())

    log.info("Removing all storage data")
    for(storage <- storageList){
      removeStorageData(storage)
    }

    log.info("Recreating storages from configuration")
    factories.values.foreach(createExistentStoragesForFactory(_))

    log.info("Reseting storage dispatcher")
    storageDispatcher.reset(configAccessor.load)
  }

  private def registerStorage(storage:Storage){
    storages.put(storage.id, storage)

    volumeManager register storage
  }

  private def unregisterStorage(storage:Storage){

    storages.remove(storage.id)
    volumeManager unregister storage
  }


  private def updateDispatcher(){
    storageDispatcher.setStorageParams(configAccessor.load)
  }

  private def isIdCorrect(newId:String):Boolean = {

    log info "Checking id '" + newId + "' for existence in platform params"

    val storageParams = configAccessor.load

    !storageParams.exists(param => param.containsId(newId))
  }

  private def removeStorageFromParams(storage:Storage){
    configAccessor.update(storageParams => storageParams.filter(_.id != storage.id))

    updateDispatcher()
  }

  private def addStorageToParams(storage:Storage){
    configAccessor.update(storageParams => storage.params :: storageParams)
    updateDispatcher()
  }

  private def createExistentStoragesForFactory(factory:StorageFactory){
    val storageParams = configAccessor.load

    log info "Existent storages: " + storageParams

    log info "Looking for existent storages for factory: " + factory.name

    for(param <- storageParams){

      if(param.storageType.equals(factory.name)){
        log info "Restoring existent storage: " + param.toString
        registerStorage(factory.createStorage(param, systemId))
      }
    }
  }

  private def removeStorageData(storage:Storage) {

    log.info("Going to remove storage data dir " + storage.fullPath)

    Files.walkFileTree(storage.fullPath.file.toPath, new SimpleFileVisitor[NioPath]{
      override def visitFile(file:NioPath, attrs:BasicFileAttributes):FileVisitResult = {
        log.info("Deleting path " + file)
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir:NioPath, e:IOException):FileVisitResult = {
        log.info("Deleting directory " + dir)

        Files.delete(dir)
        FileVisitResult.CONTINUE
      }
    })
  }

  private def getSystemId:String = {
    platformConfigManager.getPlatformProperties.get(Constants.C3_SYSTEM_ID) match {
      case Some(value) => value
      case None => throw new ConfigurationException("Failed to get systemId from params")
    }
  }

}
