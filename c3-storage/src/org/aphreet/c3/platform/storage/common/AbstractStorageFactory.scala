package org.aphreet.c3.platform.storage.common

import scala.collection.mutable.HashSet
import scala.collection.Set

import org.apache.commons.logging.LogFactory

import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.{PostConstruct, PreDestroy}

abstract class AbstractStorageFactory extends StorageFactory{

  val log = LogFactory.getLog(getClass)
  
  val createdStorages : HashSet[Storage] = new HashSet
  
  var storageManager :StorageManager = null
  
  @Autowired
  def setStorageManager(_manager:StorageManager) = {storageManager = _manager}
  
  
  def createStorage(params:StorageParams):Storage = {
    val storage = createNewStorage(params)
    
    storage.mode = params.mode
    
    createdStorages + storage
    storage
  }
  
  def storages:Set[Storage] = createdStorages
  
  
  protected def createNewStorage(params:StorageParams):Storage
  
  @PostConstruct
  def init = {
    log info "Starting " + this.name + " storage factory"
    storageManager.registerFactory(this)
  }
  
  @PreDestroy
  def destroy = {
    log info "Stopping " + this.name + " storage factory"
    
    createdStorages.foreach(s => s.mode = new U())
    
    storageManager.unregisterFactory(this)
    
    createdStorages.foreach(s => s.close)
  }
}
