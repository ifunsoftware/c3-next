package org.aphreet.c3.platform.management

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.storage.{StorageManager, Storage, StorageMode}

import org.aphreet.c3.platform.task.{TaskDescription, TaskState}

import java.util.{Map => JMap}

trait PlatformManagementEndpoint {
  
  def listStorages:List[Storage]
  
  def listStorageTypes:List[String]
  
  def createStorage(storageType:String, path:String)
  
  def removeStorage(is:String)
  
  def migrateFromStorageToStorage(sourceId:String, targetId:String)
  
  def setStorageMode(id:String, mode:StorageMode)
  
  def getPlatformProperties:JMap[String, String]
  
  def setPlatformProperty(key:String, value:String)

  def listTasks:List[TaskDescription]
 
  def setTaskMode(taskId:String, state:TaskState)
  
  def registerPropertyListener(listener:PlatformPropertyListener)
  
  def unregisterPropertyListener(listener:PlatformPropertyListener)
  
}
