package org.aphreet.c3.platform.management.impl

import java.util
import java.util.{Map => JMap}
import org.aphreet.c3.platform.common.{Logger, Path}
import org.aphreet.c3.platform.config.{PlatformConfigComponent, SetPropertyMsg}
import org.aphreet.c3.platform.management.{PlatformManagementComponent, PlatformManagementEndpoint}
import org.aphreet.c3.platform.statistics.impl.StatisticsComponentImpl
import org.aphreet.c3.platform.storage.dispatcher.selector.mime._
import org.aphreet.c3.platform.storage.impl.StorageComponentImpl
import org.aphreet.c3.platform.storage.migration.impl.MigrationComponentImpl
import org.aphreet.c3.platform.storage.{StorageIndex, Storage, StorageMode}
import org.aphreet.c3.platform.task._

trait PlatformManagementComponentImpl extends PlatformManagementComponent{

  this: TaskComponent
    with StatisticsComponentImpl
    with MimeTypeStorageSelectorComponent
    with PlatformConfigComponent
    with StorageComponentImpl
    with MigrationComponentImpl =>

  val platformManagementEndpoint: PlatformManagementEndpoint = new PlatformManagementEndpointImpl

  class PlatformManagementEndpointImpl extends PlatformManagementEndpoint{

    val log = Logger(getClass)

    {
      log.info("Starting PlatformManagementEndpoint")
    }

    def listStorages:List[Storage] = storageManager.listStorages

    def listStorageTypes:List[String] = storageManager.listStorageTypes

    def createStorage(storageType:String, path:String): Storage = {
      val storagePath = if(path == null || path.isEmpty) None else Some(Path(path))

      storageManager.createStorage(storageType, storagePath)
    }

    def removeStorage(id:String) {
      storageManager.storageForId(id).map(storageManager.removeStorage)
    }

    def purgeStorageData() {
      storageManager.resetStorages()
    }

    def setStorageMode(id:String, mode:StorageMode) {
      storageManager.setStorageMode(id, mode)
    }

    def migrateFromStorageToStorage(sourceId:String, targetId:String) {
      migrationManager.migrateStorageToStorage(sourceId, targetId)
    }

    def getPlatformProperties:JMap[String, String] = {

      val properties = new util.HashMap[String, String]

      platformConfigManager.getPlatformProperties.foreach{e => properties.put(e._1, e._2)}

      properties
    }

    def setPlatformProperty(key:String, value:String) {

      if(key == null || value == null){
        throw new NullPointerException("Properties must be not-null")
      }

      platformConfigManager ! SetPropertyMsg(key, value)
    }

    def listTasks:List[TaskDescription] = taskManager.taskList

    def listFinishedTasks:List[TaskDescription] = taskManager.finishedTaskList

    def setTaskMode(taskId:String, state:TaskState) {
      state match {
        case PAUSED => taskManager.pauseTask(taskId)
        case RUNNING => taskManager.resumeTask(taskId)
        case _ =>
      }
    }

    def listScheduledTasks = taskManager.scheduledTaskList

    def rescheduleTask(id: String, crontabSchedule: String) {
      taskManager.rescheduleTask(id, crontabSchedule)
    }

    def removeScheduledTask(id: String) {
      taskManager.removeScheduledTask(id)
    }

    def listTypeMappings:List[(String, Boolean)] = {
      mimeStorageSelector.configEntries
    }

    def addTypeMapping(mapping:(String, Boolean)) {
      mimeStorageSelector.addEntry(mapping)
    }

    def removeTypeMapping(mimeType:String) {
      mimeStorageSelector.removeEntry(mimeType)
    }

    def statistics:Map[String,String] = statisticsManager.fullStatistics

    def createIndex(index:StorageIndex) {
      storageManager.createIndex(index)
    }

    def removeIndex(name:String) {
      storageManager.removeIndex(name)
    }

  }
}
