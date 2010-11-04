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

package org.aphreet.c3.platform.remote.replication.impl.data.queue

import org.aphreet.c3.platform.task.Task
import org.aphreet.c3.platform.storage.StorageManager
import actors.Actor
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.resource.AddressGenerator
import org.aphreet.c3.platform.exception.{StorageNotFoundException}
import org.aphreet.c3.platform.remote.replication.impl.data._
import org.aphreet.c3.platform.remote.replication.ReplicationException
import org.aphreet.c3.platform.remote.replication.impl.ReplicationManagerImpl

class ReplicationQueueReplayTask(val replicationManager:ReplicationManagerImpl,
                                 val storageManager:StorageManager,
                                 val queueStorage:ReplicationQueueStorage,
                                 val sourceActor:Actor) extends Task {

  var iterator = queueStorage.iterator

  override def step{

    for(i <- 1 to 100){
      if(iterator.hasNext){
        val task = iterator.next
        iterator.remove

        submitTask(task)
      }
    }

  }

  private def submitTask(task:ReplicationTask) = {

    if(task.action == DeleteAction){

      sourceActor ! ReplicationReplayDelete(task.address, task.systemId)

    }else{
      try{
        val storage = storageManager.storageForId(AddressGenerator.storageForAddress(task.address))

        if(!storage.mode.allowRead){
          throw new ReplicationException("Storage is not readable")
        }

        val resource = storage.get(task.address) match {
          case Some(r) => r
          case None => throw new RuntimeException("Resource not found")
        }

        task.action match {
          case AddAction => sourceActor ! ReplicationReplayAdd(resource, task.systemId)
          case UpdateAction(timestamp) => sourceActor ! ReplicationReplayUpdate(resource, task.systemId)
          case DeleteAction => //Do nothing. This line is just for compiler
        }

      }catch{
        case e:StorageNotFoundException => log.error("Failed to get resource, storage not found " + task, e)
        case e => log.error("Failed to read resource, " + task, e)
      }
    }

  }

  override def shouldStop:Boolean = !iterator.hasNext

  protected override def postComplete = {
    replicationManager.isTaskRunning = false
    if(iterator != null){
      iterator.close
      iterator = null
    }
  }

  protected override def postFailure = {
    replicationManager.isTaskRunning = false
    if(iterator != null){
      iterator.close
      iterator = null
    }
  }

}