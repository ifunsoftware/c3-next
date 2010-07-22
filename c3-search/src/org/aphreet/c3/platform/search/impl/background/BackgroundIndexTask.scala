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

package org.aphreet.c3.platform.search.impl.background

import org.aphreet.c3.platform.task.Task
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.storage.{Storage, StorageIterator, StorageManager}
import org.aphreet.c3.platform.search.SearchManager
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.access.ResourceUpdatedMsg
import java.util.Date

class BackgroundIndexTask(val storageManager: StorageManager, val searchManager:SearchManager) extends Task {

  val indexedTimeout:Long = 1000 * 60 * 60

  var iterator:StorageIterator = null

  var currentStorage:Storage = null

  var storagesToIndex = storageManager.listStorages

  override def preStart = {
    log info "Starting BackgroundIndexTask"
  }

  override def step {

    if(iterator == null){
      initIterator
    }else{
      try{
        if(iterator.hasNext){
          val resource = iterator.next
          if(shouldIndex(resource)){
            log debug "Resource " + resource.address + " should be indexed"
            searchManager ! ResourceUpdatedMsg(resource)
          }
        }else{
          log debug "Iteration over storage " + currentStorage.id + " has competed"
          iterator.close
          iterator = null
        }
      }catch{
        case e:StorageException => {
          iterator.close
          iterator = null
        }
      }
    }

    Thread.sleep(1000)
  }

  private def shouldIndex(resource:Resource):Boolean = {

    def isOutOfTimeout(date:Date):Boolean = {
      System.currentTimeMillis - date.getTime > indexedTimeout
    }


    resource.systemMetadata.get("indexed") match {
      case Some(x) => false
      case None => isOutOfTimeout(resource.versions.last.date)
    }
  }

  private def initIterator = {

    if(storagesToIndex.size > 0){
      currentStorage = storagesToIndex.head
      storagesToIndex = storagesToIndex.tail
      iterator = currentStorage.iterator
      log debug "Starting iteration over storage " + currentStorage.id
    }else{
      storagesToIndex = storageManager.listStorages
    }
  }

  override def name = "BackgroundIndexer"
}