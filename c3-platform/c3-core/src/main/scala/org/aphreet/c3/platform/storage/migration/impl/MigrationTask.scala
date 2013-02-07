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
package org.aphreet.c3.platform.storage.migration.impl

import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.task.Task
import org.aphreet.c3.platform.exception.StorageException

class MigrationTask(val source:Storage, val target:Storage, val manager:StorageManager) extends Task{

  var iterator:StorageIterator = _
  
  override def preStart() {
    iterator = source.iterator()
  }
  
  override def step() {
    val resource = iterator.next
    target.update(resource)
  }
  
  override def postComplete() {
    iterator.close()
    iterator = null

    manager.mergeStorages(source.id, target.id)
    
    target.mode = new RW
    manager updateStorageParams target
    
    source.mode = new U(STORAGE_MODE_MIGRATION)
    manager removeStorage source
  }
  
  override def postFailure() {
    try{
      iterator.close()
      iterator = null
    }catch{
      case e: Throwable => log error e
    }
    
    target.mode = new RW
    source.mode = new RW
    
    manager updateStorageParams source
    manager updateStorageParams target
  }
  
  override def shouldStop:Boolean = !iterator.hasNext
  
  override def progress:Int = {
    if(iterator != null){
      val toProcess:Float = iterator.objectsProcessed
      val total:Float = source.count

      if(total > 0){
        val overalProgress = toProcess * 100 /total
        overalProgress.intValue
      }else{
        0
      }
     }else -1
  }
  
  override def finalize() {
    if(iterator != null)
      try{
        iterator.close()
      }catch{
        case e: Throwable => e.printStackTrace()
      }
  }
}
