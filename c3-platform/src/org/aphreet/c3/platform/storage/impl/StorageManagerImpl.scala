package org.aphreet.c3.platform.storage.impl;

import org.apache.commons.logging.LogFactory;

import scala.collection.mutable.HashMap

import java.io.OutputStream

import org.aphreet.c3.platform.resource.{Resource, DataWrapper}
import dispatcher.StorageDispatcher
import dispatcher.impl.{DefaultStorageDispatcher}

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.config.PlatformConfigManager;
import org.aphreet.c3.platform.storage.volume.VolumeManager

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.PostConstruct

import eu.medsea.mimeutil.MimeUtil

@Component("storageManager")
class StorageManagerImpl extends StorageManager{

  val log = LogFactory.getLog(getClass)
  
  private var storageDispatcher:StorageDispatcher = new DefaultStorageDispatcher(List())
  
  private val storages = new HashMap[String, Storage]
  
  private val factories = new HashMap[String, StorageFactory]
  
  var configManager : PlatformConfigManager = null
  
  var volumeManager : VolumeManager = null
  
  @Autowired
  def setPlatformConfigManager(manager:PlatformConfigManager) = {configManager = manager}
  
  @Autowired
  def setVolumeManager(manager:VolumeManager) = {volumeManager = manager}
  
  @PostConstruct
  def init{
	  configManager.getPlatformParam.get("c3.platform.mime.detector") match {
	    case Some(detectorClass) => MimeUtil.registerMimeDetector(detectorClass)
	    case None => null
	  }
  }
  
  
  
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
        
        factory.createStorage(new StorageParams(stId, List(), storagePath, factory.name, RW))
        
      }
      case None => throw new StorageException("Can't find factory for type: " + storageType)
    }
    
    registerStorage(storage)
    addStorageToParams(storage)
  }

  
  def listStorages:List[Storage] = List.fromIterator((for((key, s) <- storages) yield s).elements)
  
  
  
  def removeStorage(id:String) = {
    log warn "Storage remove is not implemented yet"
  }
  
  
  
  def listStorageTypes:List[String] = 
    List.fromIterator((for((key, factory) <- factories) yield factory.name).elements)
  
  
  
  def dispatcher:StorageDispatcher = storageDispatcher
  
  
  def setStorageMode(id:String, mode:StorageMode) {
    storages.get(id) match {
      case Some(s) => {
        val currentMode = s.mode
        if(currentMode == U || currentMode == RO){
          throw new StorageException("User can't change current mode")
        }else{
          s.mode = mode
        }
      }
      case None => throw new StorageException("No storage with id " + id)
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
    storageDispatcher = new DefaultStorageDispatcher(List.fromIterator((for((id, s) <- storages) yield s).elements))
  }
  
  
  
  private def isIdCorrect(newId:String):Boolean = {
    
    log info "Checking id '" + newId + "' for existence in platform params"
    
    val storageParams = configManager.getStorageParams
    
    !storageParams.exists(param => param.containsId(newId))
  }
  

  
  private def addStorageToParams(storage:Storage){
    val storageParams = configManager.getStorageParams
    
    configManager.setStorageParams(storage.params :: storageParams)
  }
  
  
  
  private def createExitentStoragesForFactory(factory:StorageFactory){
    val storageParams = configManager.getStorageParams
    
    log info "Exitent storages: " + storageParams
    
    log info "Looking for existent storages for factory: " + factory.name
    
    for(param <- storageParams){
      
      if(param.storageType.equals(factory.name)){
    	log info "Restoring existent storage: " + param.toString  
        registerStorage(factory.createStorage(param))
      }
      
    }
    
  }

}
