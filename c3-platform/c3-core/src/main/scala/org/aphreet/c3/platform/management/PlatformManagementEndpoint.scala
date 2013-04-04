package org.aphreet.c3.platform.management


import org.aphreet.c3.platform.task.{TaskDescription, TaskState}

import java.util.{Map => JMap}
import org.aphreet.c3.platform.storage.{StorageIndex, Storage, StorageMode}


trait PlatformManagementEndpoint {
  

  def listStorages:List[Storage]
  
  def listStorageTypes:List[String]
  
  def createStorage(storageType:String, path:String)
  
  def removeStorage(is:String)
  
  def migrateFromStorageToStorage(sourceId:String, targetId:String)
  
  def setStorageMode(id:String, mode:StorageMode)

  def purgeStorageData()
  

  def getPlatformProperties:JMap[String, String]
  
  def setPlatformProperty(key:String, value:String)


  def listTasks:List[TaskDescription]

  def listFinishedTasks:List[TaskDescription]

  def listScheduledTasks: List[TaskDescription]

  def rescheduleTask(id: String, crontabSchedule: String)

  def removeScheduledTask(id: String)
 
  def setTaskMode(taskId:String, state:TaskState)

  
  def listTypeMappings:List[(String, Boolean)]
  
  def addTypeMapping(mapping:(String, Boolean))
  
  def removeTypeMapping(mimeType:String)


  def statistics:Map[String,String]

  def createIndex(index:StorageIndex)

  def removeIndex(name:String)

}
