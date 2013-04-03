package org.aphreet.c3.platform.storage.updater.impl

import org.aphreet.c3.platform.storage.updater.{Transformation, StorageUpdater}
import org.aphreet.c3.platform.task.TaskManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.storage.StorageManager

@Component
class StorageUpdaterImpl extends StorageUpdater{

  @Autowired
  var taskManager: TaskManager = _

  @Autowired
  var storageManager: StorageManager = _

  def applyTransformation(transformation: Transformation) {
    taskManager.submitTask(new StorageTransformTask(storageManager.listStorages, List(transformation)))
  }
}
