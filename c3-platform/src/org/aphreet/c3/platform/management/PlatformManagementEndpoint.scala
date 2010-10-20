package org.aphreet.c3.platform.management

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.task.{TaskDescription, TaskState}

import java.util.{Map => JMap}
import org.aphreet.c3.platform.storage.volume.Volume
import org.aphreet.c3.platform.storage.{StorageIndex, StorageManager, Storage, StorageMode}

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


  def listVolumes:List[Volume]
  
  
  def listTypeMappings:List[(String, String, Boolean)]
  
  def addTypeMapping(mapping:(String, String, Boolean))
  
  def removeTypeMapping(mimeType:String)

  
  def listSizeMappings:List[(Long, String, Boolean)]
  
  def addSizeMapping(mapping:(Long, String, Boolean))
  
  def removeSizeMaping(size:Long)

  def statistics:Map[String,String]

  def createIndex(id:String, index:StorageIndex)

  def removeIndex(id:String, name:String)

  def addStorageSecondaryId(id:String, secId:String)

}
