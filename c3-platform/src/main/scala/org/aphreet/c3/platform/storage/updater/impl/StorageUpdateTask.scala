/**
 * Copyright (c) 2011, Mikhail Malygin
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
package org.aphreet.c3.platform.storage.updater.impl

import org.aphreet.c3.platform.storage.updater.Transformation
import org.aphreet.c3.platform.task.Task
import org.aphreet.c3.platform.storage.StorageIterator
import org.aphreet.c3.platform.storage.Storage

class StorageUpdateTask(val storages:List[Storage], val transformations:List[Transformation]) extends Task{

  var storagesToProcess = storages
  var currentStorage:Storage = null
  var currentIterator:StorageIterator = null

  var totalObjectsToProcess = 0l
  var processedObjects = 0l


  override def preStart(){
    totalObjectsToProcess = storages.foldLeft(0l)(_ + _.count)

    log.info("Starting StorageUpdate task. Estimated entries to process " + totalObjectsToProcess)
  }

  override def step(){

    if(currentStorage == null){
      currentStorage = selectStorage match {
        case Some(storage) => {
          log.info("Starting update for storage with id " + storage.id)
          storage
        }
        case None => {
          log.info("All storages processed")
          shouldStopFlag = true
          null
        }
      }
    }else{
      processNextResource()
    }

  }

  def selectStorage:Option[Storage] = {

    val s = storagesToProcess.headOption

    storagesToProcess = storagesToProcess.tail
    
    s
  }

  def processNextResource(){
    if(currentIterator == null){
      currentIterator = currentStorage.iterator()
    }

    if(currentIterator.hasNext){
      val resource = currentIterator.next
      transformations.foreach(t => t(resource))
      currentStorage.update(resource)
    }else{

      log.info("Storage " + currentStorage.id + " processing complete")

      processedObjects = processedObjects + currentIterator.objectsProcessed
      currentIterator.close()
      currentIterator = null
      currentStorage = null
    }
  }

  override def progress:Int = {
    if(currentIterator != null){
      ((processedObjects + currentIterator.objectsProcessed)/totalObjectsToProcess.toFloat).toInt * 100
    }else{
      ((processedObjects.toFloat)/totalObjectsToProcess).toInt * 100
    }
  }

  override def postFailure(){
    if(currentIterator != null){
      currentIterator.close()
    }
  }

}