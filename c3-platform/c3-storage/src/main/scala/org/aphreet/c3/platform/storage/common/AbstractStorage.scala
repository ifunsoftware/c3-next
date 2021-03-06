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
package org.aphreet.c3.platform.storage.common

import org.aphreet.c3.platform.storage.{Storage, StorageParams}
import org.aphreet.c3.platform.common.{Logger, ThreadWatcher, Path}
import collection.mutable
import org.aphreet.c3.platform.resource.Resource

abstract class AbstractStorage(val parameters:StorageParams, val systemId:String) extends Storage{

  val dataLog = Logger("c3.data")

  protected var counter:Thread = null

  def id:String = parameters.id

  def path:Path = parameters.path

  var indexes = parameters.indexes

  def params:StorageParams = {
    new StorageParams(id, parameters.path, parameters.storageType, mode, indexes, new mutable.HashMap[String, String])
  }

  def startObjectCounter() {
    counter = new Thread(new ObjectCounter(this))
    counter.setDaemon(true)
    counter.start()
    log.info("Started object counter for storage {}", this.id)
  }

  protected def updateObjectCount()

  override def close(){
    counter.interrupt()
  }

  def iterator = iterator(Map(), Map(), (resource:Resource) => true)

  class ObjectCounter(val storage:AbstractStorage) extends Runnable {

    override def run(){
      ThreadWatcher + this
      try{

        try{
          Thread.sleep(60 * 1000)
        }catch{
          case e: Throwable => {
            log info "Object counter for storage " + storage.id + " interrupted on start"
            return
          }
        }
        while(!Thread.currentThread.isInterrupted){
          storage.updateObjectCount()
          try{
            Thread.sleep(60 * 1000)
          }catch{
            case e:InterruptedException => {
              log.info("Object counter for storage " + storage.id + " has been interrupted")
              return
            }
          }
        }
      }finally{
        ThreadWatcher - this
      }

    }

  }

}


