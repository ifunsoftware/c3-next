package org.aphreet.c3.platform.storage.impl;

import org.apache.commons.logging.LogFactory;

import scala.collection.mutable.HashMap

import java.io.{File, OutputStream}

import org.aphreet.c3.platform.resource.{Resource, DataWrapper}
import dispatcher.StorageDispatcher
import dispatcher.impl.{DefaultStorageDispatcher}

import org.aphreet.c3.platform.common.{Path, Constants}
import org.aphreet.c3.platform.config.PlatformConfigManager;
import org.aphreet.c3.platform.storage.volume.VolumeManager

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.PostConstruct


@Component("storageManager")
class StorageManagerImpl extends StorageManager{

  val log = LogFactory.getLog(getClass)
  
  private var storageDispatcher:StorageDispatcher = null
  
  private val storages = new HashMap[String, Storage]
  
  private val factories = new HashMap[String, StorageFactory]
  
  var configAccessor : StorageConfigAccessor = null
  
  var volumeManager : VolumeManager = null
  
  @Autowired
  def setConfigAccessor(accessor:StorageConfigAccessor) = {configAccessor = accessor}
  
  @Autowired
  def setVolumeManager(manager:VolumeManager) = {volumeManager = manager}
  
  @Autowired
  def setStorageDispatcher(dispatcher:StorageDispatcher) = {storageDispatcher = dispatcher}
  
  def registerFactory(factory:StorageFactory) = {
    factories.synchronized{
    	factories.put(factory.name, factory)
    }
    
    createExitentStoragesForFactory(factory)
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
	  case None => throw new StorageException("Can't find storage for id: " + id)
	}
  }
  
  def createStorage(storageType:String, storagePath:Path){
    val storage = factories.get(storageType) match {
      case Some(factory) => {
        
        val rand = new scala.util.Random
        var stId = ""
        
        do{
          stId = Integer.toHexString((Math.abs(rand.nextInt) % 0xEFFF) + 0x1000)
        }while(!isIdCorrect(stId))
        
        log info "Creating new storage with id: " + stId
        
        factory.createStorage(new StorageParams(stId, List(), storagePath, factory.name, RW(Constants.STORAGE_MODE_NONE)))
        
      }
      case None => throw new StorageException("Can't find factory for type: " + storageType)
    }
    
    registerStorage(storage)
    addStorageToParams(storage)
  }

  
  def listStorages:List[Storage] = 
	List.fromIterator(storages.map(_._2).elements).removeDuplicates
  
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
    List.fromIterator((for((key, factory) <- factories) yield factory.name).elements)
  
  def dispatcher:StorageDispatcher = storageDispatcher
  
  
  def setStorageMode(id:String, mode:StorageMode) {
    storages.get(id) match {
      case Some(s) => {
        s.mode = mode
        updateStorageParams(s)
      }
      case None => throw new StorageException("No storage with id " + id)
    }
  }
  
  def updateStorageParams(storage:Storage) {
    
    configAccessor.update(config => storage.params :: config.filter(_.id != storage.id))
    
    for(id <- storage.id :: storage.ids){
      storages.put(id, storage)
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
    storageDispatcher.setStorages(storages.map((entry:(String, Storage)) => entry._2).elements.toList)
  }
  
  
  
  private def isIdCorrect(newId:String):Boolean = {
    
    log info "Checking id '" + newId + "' for existence in platform params"
    
    val storageParams = configAccessor.load
    
    !storageParams.exists(param => param.containsId(newId))
  }
  
  private def addStorageToParams(storage:Storage){
    configAccessor.update(storageParams => storage.params :: storageParams)
  }
  
  
  
  private def createExitentStoragesForFactory(factory:StorageFactory){
    val storageParams = configAccessor.load
    
    log info "Exitent storages: " + storageParams
    
    log info "Looking for existent storages for factory: " + factory.name
    
    for(param <- storageParams){
      
      if(param.storageType.equals(factory.name)){
    	log info "Restoring existent storage: " + param.toString  
        registerStorage(factory.createStorage(param))
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
