package org.aphreet.c3.platform.storage.updater.impl

import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.storage.StorageManager
import org.aphreet.c3.platform.storage.updater.{Transformation, StorageUpdater}
import org.aphreet.c3.platform.task.TaskManager


class StorageUpdaterImpl(val storageManager: StorageManager, val taskManager: TaskManager) extends StorageUpdater{

  val log = Logger(classOf[StorageUpdaterImpl])

  {
    log.info("Starting StorageUpdater")
  }

  def applyTransformation(transformation: Transformation) {
    taskManager.submitTask(new StorageTransformTask(storageManager.listStorages, List(transformation)))
  }
}
