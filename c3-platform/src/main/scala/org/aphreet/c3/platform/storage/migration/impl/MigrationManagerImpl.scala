package org.aphreet.c3.platform.storage.migration.impl

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.common.Constants
import org.aphreet.c3.platform.exception.MigrationException
import org.aphreet.c3.platform.task.TaskManager

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.storage.migration.MigrationManager

@Component
class MigrationManagerImpl extends MigrationManager{

  private var storageManager:StorageManager = null
  
  private var taskManager:TaskManager = null
  
  val log = LogFactory getLog getClass
  
  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}
  
  @Autowired
  def setTaskManager(manager:TaskManager) = {taskManager = manager}
  
  def migrateStorageToStorage(sourceId:String, targetId:String) = {
    val source = storageManager storageForId sourceId
    val target = storageManager storageForId targetId
    
    if(source != null && target != null){
      migrateStorageToStorage(source, target)
    }else{
      throw new MigrationException("Can't find one of storages")
    }
    
  }
  
  
  def migrateStorageToStorage(source:Storage, target:Storage) = {
    
    log info "Starting migration from " + source.name + " " + source.id + " to " + target.name + " " + target.id
    
    try{
      
      checkPreconditions(source, target)
      source.mode = RO(Constants.STORAGE_MODE_MIGRATION)
      target.mode = RW(Constants.STORAGE_MODE_MIGRATION)
      
      val migration = new MigrationTask(source, target, storageManager)
      
      taskManager submitTask migration
      
    }catch{
      case e:MigrationException=> log.error(e.message, e)
      throw e
    }
  }
  
  private def checkPreconditions(source:Storage, target:Storage) = {
  
    log info "Checking preconditions"
    
    if(!source.mode.allowRead){
      throw new MigrationException("Source is not readable")
    }
    
    if(!target.mode.allowWrite){
      throw new MigrationException("Target is not writable")
    }
    
    val requiredFreeCapacity = source.size
    
    val freeCapacityOnTarget = target.volume.safeAvailable
    
    if(freeCapacityOnTarget < requiredFreeCapacity){
      throw new MigrationException("Not enought capacity on target")
    }
    
    log info "Preconditions check complete"
    
  }
  
}
