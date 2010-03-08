package org.aphreet.c3.platform.storage.query.impl

import org.aphreet.c3.platform.storage.query.QueryManager
import java.io.File
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.storage.StorageManager
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.task.TaskManager
import org.aphreet.c3.platform.exception.PlatformException

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 9, 2010
 * Time: 12:09:46 AM
 * To change this template use File | Settings | File Templates.
 */

@Component
class QueryManagerImpl extends QueryManager{

  var storageManager:StorageManager = _
  var taskManager:TaskManager = _


  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}

  @Autowired
  def setTaskManager(manager:TaskManager) = {taskManager = manager}


  def buildResourceList(dir:File){
    if(!dir.isDirectory) throw new PlatformException(dir.getAbsolutePath + " is not directry")

    val storages = storageManager.listStorages

    for(storage <- storages){
      val task = new ResourceListTask(storage, new File(dir, storage.id + ".out"))
      taskManager.submitTask(task)
    }

  }

}