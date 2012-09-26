package org.aphreet.c3.platform.management.impl

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.storage.migration._
import org.aphreet.c3.platform.storage.dispatcher.selector.mime._
import org.aphreet.c3.platform.task._

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired


import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.aphreet.c3.platform.config.{SetPropertyMsg, PlatformConfigManager}
import java.util.{HashMap, Map => JMap}
import org.aphreet.c3.platform.statistics.StatisticsManager
import org.aphreet.c3.platform.storage.volume.{Volume, VolumeManager}
import org.aphreet.c3.platform.storage.{StorageIndex, StorageManager, Storage, StorageMode}

@Component("platformManagementEndpoint")
class PlatformManagementEndpointImpl extends PlatformManagementEndpoint{

  val log = LogFactory.getLog(getClass)

  @Autowired
  var storageManager:StorageManager = null

  @Autowired
  var taskManager:TaskManager = null

  @Autowired
  var migrationManager:MigrationManager = null

  @Autowired
  var mimeSelector:MimeTypeStorageSelector = null

  @Autowired
  var configManager:PlatformConfigManager = _

  @Autowired
  var statisticsManager:StatisticsManager = _

  @Autowired
  var volumeManager:VolumeManager = _

  def listStorages:List[Storage] = storageManager.listStorages
  
  def listStorageTypes:List[String] = storageManager.listStorageTypes
  
  def createStorage(storageType:String, path:String) {
    storageManager.createStorage(storageType, new Path(path))
  }
  
  def removeStorage(id:String) {
    val storage = storageManager.storageForId(id)
    if(storage != null){
      storageManager.removeStorage(storage)
    }else{
      throw new StorageException("Can't find storage for id")
    }
  }
  
  def setStorageMode(id:String, mode:StorageMode) {
    storageManager.setStorageMode(id, mode)
  }
 
  def migrateFromStorageToStorage(sourceId:String, targetId:String) {
    migrationManager.migrateStorageToStorage(sourceId, targetId)
  }
  
  def getPlatformProperties:JMap[String, String] = {

    val properties = new HashMap[String, String]

    configManager.getPlatformProperties.foreach{e => properties.put(e._1, e._2)}

    properties
  }

  def setPlatformProperty(key:String, value:String) {

    if(key == null || value == null){
      throw new NullPointerException("Properties must be not-null")
    }

    configManager ! SetPropertyMsg(key, value)
  }
  
  def listTasks:List[TaskDescription] = taskManager.taskList

  def listFinishedTasks:List[TaskDescription] = taskManager.finishedTaskList
  
  def setTaskMode(taskId:String, state:TaskState) {
    state match {
      case PAUSED => taskManager.pauseTask(taskId)
      case RUNNING => taskManager.resumeTask(taskId)
      case _ => null
    }
  }
  
  def listTypeMappings:List[(String,String,Boolean)] = {
    mimeSelector.configEntries
  }
  
  def addTypeMapping(mapping:(String, String, Boolean)) {
    mimeSelector.addEntry(mapping)
  }
  
  def removeTypeMapping(mimeType:String) {
    mimeSelector.removeEntry(mimeType)
  }


  def statistics:Map[String,String] = statisticsManager.fullStatistics

  def listVolumes:List[Volume] = volumeManager.volumeList

  def createIndex(id:String, index:StorageIndex) {
    storageManager.createIndex(id, index)
  }

  def removeIndex(id:String, name:String) {
    storageManager.removeIndex(id, name)
  }

}
