/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.platform.storage.query.impl

import java.io.File
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.storage.StorageManager
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.task.TaskManager
import org.aphreet.c3.platform.exception.PlatformException
import org.aphreet.c3.platform.storage.query.QueryManager
import org.aphreet.c3.platform.access.QueryConsumer

@Component
class QueryManagerImpl extends QueryManager{

  var storageManager:StorageManager = _
  var taskManager:TaskManager = _


  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}

  @Autowired
  def setTaskManager(manager:TaskManager) = {taskManager = manager}


  override
  def buildResourceList(dir:File){
    if(!dir.isDirectory) throw new PlatformException(dir.getAbsolutePath + " is not directry")

    val storages = storageManager.listStorages

    for(storage <- storages){
      val task = new ResourceListTask(storage, new File(dir, storage.id + ".out"))
      taskManager.submitTask(task)
    }

  }

  override
  def executeQuery(consumer:QueryConsumer){

    val storages = storageManager.listStorages

    for(storage <- storages){
      val iterator = storage.iterator

      try{

        while(iterator.hasNext)
          consumer.addResource(iterator.next)

      }finally{
        iterator.close
      }
    } 
  }

}