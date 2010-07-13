package org.aphreet.c3.platform.management.impl

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.storage.{StorageManager, Storage, StorageMode}
import org.aphreet.c3.platform.storage.migration._
import org.aphreet.c3.platform.storage.dispatcher.selector.mime._
import org.aphreet.c3.platform.storage.dispatcher.selector.size._
import org.aphreet.c3.platform.task._

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired


import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.aphreet.c3.platform.config.{SetPropertyMsg, PlatformConfigManager}
import java.util.{HashMap, Map => JMap}
import org.aphreet.c3.platform.statistics.StatisticsManager

@Component("platformManagementEndpoint")
class PlatformManagementEndpointImpl extends PlatformManagementEndpoint{

  val log = LogFactory.getLog(getClass)
  
  var storageManager:StorageManager = null
  
  var taskManager:TaskManager = null
  
  var migrationManager:MigrationManager = null
  
  var mimeSelector:MimeTypeStorageSelector = null
  
  var sizeSelector:SizeStorageSelector = null

  var configManager:PlatformConfigManager = _

  var statisticsManager:StatisticsManager = _

  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}

  @Autowired
  def setMigrationManager(manager:MigrationManager) = {migrationManager = manager}
  
  @Autowired
  def setMimeTypeStorageSelector(selector:MimeTypeStorageSelector) = {mimeSelector = selector}
  
  @Autowired
  def setSizeStorageSelector(selector:SizeStorageSelector) = {sizeSelector = selector}

  @Autowired
  def setTaskExecutor(manager:TaskManager) = {taskManager = manager}

  @Autowired
  def setPlatformConfigManager(manager:PlatformConfigManager) = {configManager = manager}

  @Autowired
  def setStatisticsManager(manager:StatisticsManager) = {statisticsManager = manager}

  def listStorages:List[Storage] = storageManager.listStorages
  
  def listStorageTypes:List[String] = storageManager.listStorageTypes
  
  def createStorage(storageType:String, path:String) = storageManager.createStorage(storageType, new Path(path))
  
  def removeStorage(id:String) = {
    val storage = storageManager.storageForId(id)
    if(storage != null){
      storageManager.removeStorage(storage)
    }else{
      throw new StorageException("Can't find storage for id")
    }
  }
  
  def setStorageMode(id:String, mode:StorageMode) = storageManager.setStorageMode(id, mode)
 
  def migrateFromStorageToStorage(sourceId:String, targetId:String) = {
    migrationManager.migrateStorageToStorage(sourceId, targetId)
  }
  
  def getPlatformProperties:JMap[String, String] = {

    val properties = new HashMap[String, String]

    configManager.getPlatformProperties.foreach{e => properties.put(e._1, e._2)}

    properties
  }

  def setPlatformProperty(key:String, value:String) = {

    configManager ! SetPropertyMsg(key, value)
  }
  
  def listTasks:List[TaskDescription] = taskManager.taskList

  def listFinishedTasks:List[TaskDescription] = taskManager.finishedTaskList
  
  def setTaskMode(taskId:String, state:TaskState) ={
    state match {
      case PAUSED => taskManager.pauseTask(taskId)
      case RUNNING => taskManager.resumeTask(taskId)
      case _ => null
    }
  }
  
  def listTypeMappings:List[(String,String,Boolean)] = {
    mimeSelector.configEntries
  }
  
  def addTypeMapping(mapping:(String, String, Boolean)) = {
    mimeSelector.addEntry(mapping)
  }
  
  def removeTypeMapping(mimeType:String) = {
    mimeSelector.removeEntry(mimeType)
  }
  
  def listSizeMappings:List[(Long, String, Boolean)] = sizeSelector.configEntries
  
  def addSizeMapping(mapping:(Long, String, Boolean)) = sizeSelector.addEntry(mapping)
  
  def removeSizeMaping(size:Long) = sizeSelector.removeEntry(size)


  def statistics:Map[String,String] = statisticsManager.fullStatistics

}
