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

package org.aphreet.c3.platform.remote.impl

import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.remote.api.management.{Pair, SizeMapping, TypeMapping, StorageDescription, PlatformManagementAdapter, TaskDescription => RemoteTaskDescription}
import org.aphreet.c3.platform.task.{RUNNING, PAUSED, TaskState, TaskDescription}
import org.aphreet.c3.platform.storage.{U, RO, RW}
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.remote.api.RemoteException
import scala.collection.jcl.Map
import org.aphreet.c3.platform.exception.{StorageException, PlatformException}

@Component("platformManagementAdapter")
class PlatformManagementAdapterImpl extends PlatformManagementAdapter{
  val log = LogFactory getLog getClass

  var managementEndpoint:PlatformManagementEndpoint = null

  @Autowired
  def setManagementEndpoint(endPoint:PlatformManagementEndpoint)
  = {managementEndpoint = endPoint}

  def listStorages:Array[StorageDescription] = {

    catchAll(() => {
      (for(s <-managementEndpoint.listStorages)
        yield new StorageDescription(s.id,
            s.getClass.getSimpleName,
            s.path.toString,
            s.mode.name + "(" + s.mode.message + ")",
            s.count)).toArray
    })

  }

  def listStorageTypes:Array[String] =
    catchAll(() => {
      managementEndpoint.listStorageTypes.toArray
    })


  def createStorage(stType:String, path:String) =
    catchAll(() => {managementEndpoint.createStorage(stType, path)})

  def removeStorage(id:String) =
    catchAll(() => {managementEndpoint.removeStorage(id)})

  def migrate(source:String, target:String) =
    catchAll(() => {
      managementEndpoint.migrateFromStorageToStorage(source, target)
    })

  def setStorageMode(id:String, mode:String) =
    catchAll(() => {
      val storageMode = mode match {
        case "RW" => RW(STORAGE_MODE_USER)
        case "RO" => RO(STORAGE_MODE_USER)
        case "U" => U(STORAGE_MODE_USER)
        case _ => throw new StorageException("No mode named " + mode)
      }
      managementEndpoint.setStorageMode(id, storageMode)
    })


  def setPlatformProperty(key:String, value:String) =
    catchAll(() =>
      managementEndpoint.setPlatformProperty(key, value)
      )

  def platformProperties:Array[Pair] =
    catchAll(() => {
      (for(e <- Map.apply(managementEndpoint.getPlatformProperties))
        yield new Pair(e._1, e._2)).toSeq.toArray
    })

  def listTasks:Array[RemoteTaskDescription] =
    catchAll(() =>
      managementEndpoint.listTasks.map(fromLocalDescription(_)).toArray
    )

  def listFinishedTasks:Array[RemoteTaskDescription] =
    catchAll(() =>
      managementEndpoint.listFinishedTasks.map(fromLocalDescription(_)).toArray
    )

  private def fromLocalDescription(descr:TaskDescription):RemoteTaskDescription = {
    new RemoteTaskDescription(descr.id, descr.name, descr.state.name, descr.progress.toString)
  }

  def setTaskMode(taskId:String, mode:String) =
    catchAll(() => {
      val state:TaskState = mode match {
        case "pause" => PAUSED
        case "resume"   => RUNNING
        case _ => throw new PlatformException("mode is not valid")

      }

      managementEndpoint.setTaskMode(taskId, state)
    })

  def listTypeMappigs:Array[TypeMapping] =
    catchAll(() => {
      (for(entry <- managementEndpoint.listTypeMappings)
        yield new TypeMapping(entry._1, entry._2, if(entry._3) 1.shortValue else 0.shortValue )).toArray
    })

  def addTypeMapping(mimeType:String, storage:String, versioned:java.lang.Short) =
    catchAll(() => {
      val vers = versioned == 1
      managementEndpoint.addTypeMapping((mimeType, storage, vers))
    })

  def removeTypeMapping(mimeType:String) =
    catchAll(() =>
      managementEndpoint.removeTypeMapping(mimeType)
      )

  def listSizeMappings:Array[SizeMapping] =
    catchAll(() => {
      (for(entry <- managementEndpoint.listSizeMappings)
        yield new SizeMapping(entry._1, entry._2, if(entry._3) 1 else 0)).toArray
    })

  def addSizeMapping(size:java.lang.Long, storage:String, versioned:java.lang.Integer) =
    catchAll(() => {
      val vers = versioned == 1
      managementEndpoint.addSizeMapping((size.longValue, storage, vers))
    })

  def removeSizeMapping(size:java.lang.Long) =
    catchAll(() => {
      managementEndpoint.removeSizeMaping(size.longValue)
    })

  def listUsers:Array[Pair] =
    catchAll(() => {
      managementEndpoint.listUsers.map(e => new Pair(e._1, e._2)).toSeq.toArray
    })

  def addUser(name:String, password:String, role:String) = {
    catchAll(() => {
      managementEndpoint.addUser(name, password, role)
    })
  }

  def updateUser(name:String, password:String, role:String) = {
    catchAll(() => {
      managementEndpoint.updateUser(name, password,role)
    })
  }

  def deleteUser(name:String) = {
    catchAll(() => {
      managementEndpoint.deleteUser(name)
    })
  }

  private def catchAll[T](function:Function0[T]) : T = {
    try{
      function.apply
    }catch{
      case e => {
        log.error(e)
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }
}