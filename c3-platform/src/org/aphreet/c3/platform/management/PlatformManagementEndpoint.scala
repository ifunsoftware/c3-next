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

  def listFinishedTasks:List[TaskDescription]
 
  def setTaskMode(taskId:String, state:TaskState)
  
  
  
  def listTypeMappings:List[(String, String, Boolean)]
  
  def addTypeMapping(mapping:(String, String, Boolean))
  
  def removeTypeMapping(mimeType:String)

  
  def listSizeMappings:List[(Long, String, Boolean)]
  
  def addSizeMapping(mapping:(Long, String, Boolean))
  
  def removeSizeMaping(size:Long)

}
