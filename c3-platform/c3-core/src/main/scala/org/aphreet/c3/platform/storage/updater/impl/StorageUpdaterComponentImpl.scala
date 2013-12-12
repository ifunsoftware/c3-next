package org.aphreet.c3.platform.storage.updater.impl

import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.storage.{StorageComponent, StorageManager}
import org.aphreet.c3.platform.storage.updater.{StorageUpdaterComponent, Transformation, StorageUpdater}
import org.aphreet.c3.platform.task.{TaskComponent, TaskManager}

trait StorageUpdaterComponentImpl extends StorageUpdaterComponent {

  this: StorageComponent
    with TaskComponent =>

  val storageUpdater: StorageUpdater = new StorageUpdaterImpl

  class StorageUpdaterImpl extends StorageUpdater{

    val log = Logger(classOf[StorageUpdaterComponentImpl])

    {
      log.info("Starting StorageUpdater")
    }

    def applyTransformation(transformation: Transformation) {
      taskManager.submitTask(new StorageTransformTask(storageManager.listStorages, List(transformation)))
    }
  }
}
