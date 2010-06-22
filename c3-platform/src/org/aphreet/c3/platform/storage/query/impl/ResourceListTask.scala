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

import org.aphreet.c3.platform.task.Task
import org.aphreet.c3.platform.storage.{StorageIterator, Storage}
import java.io.{FileWriter, BufferedWriter, File}

class ResourceListTask(val storage:Storage, val file:File) extends Task{

  var iterator:StorageIterator = _
  var fileWriter:BufferedWriter = _

  override def preStart = {
    iterator = storage.iterator
    fileWriter = new BufferedWriter(new FileWriter(file))
  }

  override def step = {
    val resource = iterator.next
    fileWriter.write(resource.address + "\n")
  }

  override def postComplete = {
    iterator.close
    iterator = null
    fileWriter.close
    fileWriter = null
  }

  override def postFailure = {
    try{
      iterator.close
      iterator = null
      fileWriter.close
      fileWriter = null
    }catch{
      case e=> log error e
    }
  }

  override def shouldStop:Boolean = !iterator.hasNext

  override def progress:Int = {
    if(iterator != null){
      val toProcess:Float = iterator.objectsProcessed
      val total:Float = storage.count

      if(total > 0){
        val overalProgress = toProcess * 100 /total
        overalProgress.intValue
      }else{
        0
      }
     }else -1
  }

  override def finalize = {
    if(iterator != null)
      try{
        iterator.close
      }catch{
        case e => e.printStackTrace
      }
    if(fileWriter != null){
      try{
        fileWriter.close
      }catch{
        case e=> e.printStackTrace
      }
    }
  }
}