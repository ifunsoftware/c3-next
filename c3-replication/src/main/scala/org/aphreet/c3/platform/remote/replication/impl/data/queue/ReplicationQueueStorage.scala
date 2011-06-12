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

import actors.Actor
import collection.Set

import java.io._

import com.sleepycat.je._
import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.exception.PlatformException
import org.aphreet.c3.platform.remote.api.management.ReplicationHost
import org.aphreet.c3.platform.remote.replication.impl.data._
import org.aphreet.c3.platform.remote.replication.ReplicationException

class ReplicationQueueStorage(val path:Path) {

  val log = LogFactory getLog getClass

  protected var env : Environment = null

  protected var database : Database = null

  {
    open
  }

  private def open {
    log info "Opening ReplicationQueueDB..."

    val envConfig = new EnvironmentConfig
    envConfig setAllowCreate true
    envConfig setSharedCache false
    envConfig setTransactional false
    envConfig setTxnNoSync true
    envConfig setTxnWriteNoSync true

    val storagePathFile = path.file
    if(!storagePathFile.exists){
      storagePathFile.mkdirs
    }

    env = new Environment(storagePathFile, envConfig)

    val dbConfig = new DatabaseConfig
    dbConfig setAllowCreate true
    dbConfig setTransactional false
    database = env.openDatabase(null, "ReplicationQueueDB", dbConfig)

    log info "ReplicationQueueDB opened"
  }

  def add(tasks:Set[ReplicationTask]) = {
    for(task <- tasks){
      try{
        val key = new DatabaseEntry(task.getKeyBytes)
        val value = new DatabaseEntry()

        val status = database.get(null, key, value, LockMode.DEFAULT)

        status match {
          case OperationStatus.SUCCESS => {
            val queuedAction = ReplicationAction.fromBytes(value.getData)

            if(task.action.isStronger(queuedAction)){
              value.setData(task.action.toBytes)
              if(database.put(null, key, value) != OperationStatus.SUCCESS){
                log warn "Can't put task to queue " + task
              }
            }

          }

          case OperationStatus.NOTFOUND => {
            value.setData(task.action.toBytes)
            if(database.put(null, key, value) != OperationStatus.SUCCESS){
              log warn "Can't put task to queue " + task
            }
          }
        }


      }catch{
        case e => log error ("Can't add entry to database", e)
      }
    }
  }

  def iterator:ReplicationQueueIterator = new ReplicationQueueIterator(database)

  def close = {

    log info "Closing ReplicationQueueDB..."

    if(database != null){
      database.close
      database = null
    }

    if(env != null){
      env.cleanLog;
      env.close
      env = null
    }

    log info "ReplicationQueueDB closed"
  }

}

class ReplicationQueueIterator(val database:Database) extends java.util.Iterator[ReplicationTask]{

  val cursor = database.openCursor(null, null)

  private var nextElement = findNextTask

  override def hasNext:Boolean = {

    nextElement match {
      case Some(task) => true
      case None => false
    }

  }

  override def next:ReplicationTask = {

    val result = nextElement match {
      case Some(task) => task
      case None => throw new IllegalStateException
    }

    nextElement = findNextTask

    result
  }

  override def remove = {

    val key = new DatabaseEntry
    val value = new DatabaseEntry

    cursor.getPrev(key, value, LockMode.DEFAULT)

    if(cursor.delete != OperationStatus.SUCCESS){
      throw new ReplicationException("Failed to remove task from queue") 
    }

    nextElement = findNextTask
  }

  private def findNextTask:Option[ReplicationTask] = {

    val key = new DatabaseEntry
    val value = new DatabaseEntry

    if (cursor.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      Some(ReplicationTask.fromByteArrays(key.getData, value.getData))
    } else {
      None
    }
  }

  def close = {
    cursor.close
  }

}


