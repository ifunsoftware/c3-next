package org.aphreet.c3.platform.storage.common

import scala.collection.mutable.{Set, HashSet}

import org.apache.commons.logging.LogFactory

import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.{PostConstruct, PreDestroy}
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.common.ComponentGuard

abstract class AbstractStorageFactory extends StorageFactory with ComponentGuard{

  val log = LogFactory.getLog(getClass)
  
  val createdStorages : HashSet[Storage] = new HashSet
  
  var storageManager :StorageManager = null
  
  @Autowired
  def setStorageManager(_manager:StorageManager) = {storageManager = _manager}
  
  
  def createStorage(params:StorageParams, systemId:String):Storage = {
    val storage = createNewStorage(params, systemId)
    
    storage.mode = params.mode
    
    createdStorages += storage
    storage
  }

  def storages:Set[Storage] = createdStorages
  
  protected def createNewStorage(params:StorageParams, systemId:String):Storage
  
  @PostConstruct
  def init = {
    log info "Starting " + this.name + " storage factory"
    storageManager.registerFactory(this)
  }
  
  @PreDestroy
  def destroy = {
    log info "Stopping " + this.name + " storage factory"
    
    createdStorages.foreach(s => s.close)

    letItFall{
      storageManager.unregisterFactory(this)
    }
    
    createdStorages.clear
  }
}
