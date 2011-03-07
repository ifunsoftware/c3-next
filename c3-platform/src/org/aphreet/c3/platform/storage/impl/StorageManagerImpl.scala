package org.aphreet.c3.platform.storage.impl;

import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.storage._


import scala.collection.mutable.HashMap

import dispatcher.StorageDispatcher

import org.aphreet.c3.platform.common.{Path, Constants}
import org.aphreet.c3.platform.storage.volume.VolumeManager

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import org.aphreet.c3.platform.config.PlatformConfigManager
import org.aphreet.c3.platform.exception.{ConfigurationException, StorageException, StorageNotFoundException}
import actors.Actor
import actors.Actor._
import org.aphreet.c3.platform.common.msg.{DestroyMsg, UnregisterListenerMsg, RegisterListenerMsg}
import javax.annotation.{PreDestroy, PostConstruct}
import collection.immutable.HashSet
import org.aphreet.c3.platform.resource.{IdGenerator, Resource}

@Component("storageManager")
class StorageManagerImpl extends StorageManager{

  val log = LogFactory.getLog(getClass)

  private var storageDispatcher:StorageDispatcher = null

  private val storages = new HashMap[String, Storage]

  private val factories = new HashMap[String, StorageFactory]

  var configAccessor : StorageConfigAccessor = null

  var volumeManager : VolumeManager = null

  var platformConfigManager:PlatformConfigManager = null

  var systemId:String = null

  var listeners = new HashSet[Actor]

  @Autowired
  def setConfigAccessor(accessor:StorageConfigAccessor) = {configAccessor = accessor}

  @Autowired
  def setVolumeManager(manager:VolumeManager) = {volumeManager = manager}

  @Autowired
  def setStorageDispatcher(dispatcher:StorageDispatcher) = {storageDispatcher = dispatcher}

  @Autowired
  def setPlatformConfigManager(manager:PlatformConfigManager) = {platformConfigManager = manager}

  @PostConstruct
  def init{

    log info "Starting StorageManager"

    platformConfigManager.getPlatformProperties.get(Constants.C3_SYSTEM_ID) match {
      case Some(value) => systemId = value
      case None => throw new ConfigurationException("Failed to get systemId from params")
    }

    this.start

    log info "StorageManager started"
  }

  @PreDestroy
  def destroy{
    log info "Stopping StorageManager..."
    this ! DestroyMsg
  }

  def act{
    loop{
      receive{
        case RegisterListenerMsg(listener) => {
          log debug "Registering listener " + listener
          listeners = listeners - listener

          log.trace("Listeners: " + listeners)

        }
        case UnregisterListenerMsg(listener) => {
          log debug "Unregistering listener " + listener
          listeners = listeners + listener

          log.trace("Listeners: " + listeners)
        }

        case DestroyMsg => {
          log info "StorageManager stopped"
          this.exit
        }
      }
    }
  }

  def registerFactory(factory:StorageFactory) = {
    factories.synchronized{
      factories.put(factory.name, factory)
    }

    createExistentStoragesForFactory(factory)
  }

  def unregisterFactory(factory:StorageFactory) ={

    storages.synchronized{
      factory.storages.foreach(s => unregisterStorage(s))
    }

    factories.synchronized{
      factories - factory.name
    }

    updateDispatcher

  }

  def storageForId(id:String):Storage = {
    storages.get(id) match {
      case Some(storage) => storage
      case None => throw new StorageNotFoundException(id)
    }
  }

  def storageForResource(resource:Resource):Storage = {
    storageDispatcher.selectStorageForResource(resource)
  }

  def createStorage(storageType:String, storagePath:Path){
    val storage = factories.get(storageType) match {
      case Some(factory) => {

        val rand = new scala.util.Random
        var stId = ""

        do{
          stId = IdGenerator.generateStorageId
        }while(!isIdCorrect(stId))

        log info "Creating new storage with id: " + stId

        factory.createStorage(new StorageParams(stId, List(), storagePath, factory.name, RW(Constants.STORAGE_MODE_NONE), List()), systemId)

      }
      case None => throw new StorageException("Can't find factory for type: " + storageType)
    }

    registerStorage(storage)
    addStorageToParams(storage)

    listeners.foreach(_ ! StorageCreatedMsg(storage.params))
  }


  def listStorages:List[Storage] =
    storages.map(_._2).toList.distinct

  def removeStorage(storage:Storage) = {

    if(storage.count == 0
            || storage.mode == U(Constants.STORAGE_MODE_MIGRATION)){

      for((id, st) <- storages if st eq storage) {
        storages - id
      }

      factories.values.foreach(_.storages - storage)

      configAccessor.update(storageParams => storageParams.filter(_.id != storage.id))

      updateDispatcher
      storage.close

      removeStorageData(storage)

      log info "Storage with id " + storage.id + " removed"
    }else{
      throw new StorageException("Failed to remove non-empty storage")
    }
  }



  def listStorageTypes:List[String] =
    factories.map(e => e._2.name).toList

  def dispatcher:StorageDispatcher = storageDispatcher


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

    configAccessor.update(config => storage.params :: config.filter(_.id != storage.id))

    for(id <- storage.id :: storage.ids){
      storages.put(id, storage)
    }
  }

  def createIndex(id:String, index:StorageIndex) = {
    val storage = storageForId(id)

    if(storage.count != 0){
      throw new StorageException("Unable to create index on storage with content")
    }

    storage.createIndex(index)

    updateStorageParams(storage)
  }

  def removeIndex(id:String, name:String) = {
    val storage = storageForId(id)

    val indexes = storage.params.indexes.filter(_.name != name)

    if(indexes.size > 0){
      storage.removeIndex(indexes.head)
      updateStorageParams(storage)
    }else{
      throw new StorageException("Index not found")
    }

  }

  def addSecondaryId(id:String, secondaryId:String) = {

    this.synchronized{
      val storageParams = configAccessor.load

      val idNotExists = storageParams
              .filter(p => p.id == secondaryId || p.secIds.contains(secondaryId)).isEmpty

      if(idNotExists){

        storages.get(id) match{
          case Some(s) => {
            s.ids = secondaryId :: s.ids
            updateStorageParams(s)
            log debug "Appended new secondary id " + secondaryId + " to storage with id " + id

            listeners.foreach(_ ! StorageIdCreatedMsg(secondaryId, s.params.storageType))

          }
          case None => throw new StorageException("Storage with id" + id + " is not exist")
        }

      }else{
        throw new StorageException("Specified id already exists")
      }
    }
  }

  private def registerStorage(storage:Storage){
    storages.put(storage.id, storage)

    for(id <- storage.ids)
      storages.put(id, storage)

    volumeManager register storage

    updateDispatcher
  }

  private def unregisterStorage(storage:Storage){

    storages -- (storage.id :: storage.ids)
    volumeManager unregister storage
  }


  private def updateDispatcher{
    storageDispatcher.setStorages(storages.map((entry:(String, Storage)) => entry._2).toList)
  }



  private def isIdCorrect(newId:String):Boolean = {

    log info "Checking id '" + newId + "' for existence in platform params"

    val storageParams = configAccessor.load

    !storageParams.exists(param => param.containsId(newId))
  }

  private def addStorageToParams(storage:Storage){
    configAccessor.update(storageParams => storage.params :: storageParams)
  }



  private def createExistentStoragesForFactory(factory:StorageFactory){
    val storageParams = configAccessor.load

    log info "Exitent storages: " + storageParams

    log info "Looking for existent storages for factory: " + factory.name

    for(param <- storageParams){

      if(param.storageType.equals(factory.name)){
        log info "Restoring existent storage: " + param.toString
        registerStorage(factory.createStorage(param, systemId))
      }
    }
  }

  private def removeStorageData(storage:Storage) = {
    def removeDir(file:File){
      if(file.isDirectory)
        file.listFiles.foreach(removeDir(_))
      file.delete
    }
    removeDir(storage.fullPath.file)
  }

}
