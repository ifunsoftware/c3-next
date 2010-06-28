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
package org.aphreet.c3.platform.remote.api.ws.impl

import org.aphreet.c3.platform.remote.api.management._
import javax.jws.{WebMethod, WebService}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.remote.api.ws.PlatformWSManagementEndpoint

@Component
@WebService{val serviceName="ManagementService", val targetNamespace="remote.c3.aphreet.org"}
class PlatformWSManagementEndpointImpl extends PlatformWSManagementEndpoint{


  private var managementAdapter:PlatformManagementAdapter = null

  @Autowired
  private def setManagementAdapter(adapter:PlatformManagementAdapter) = {managementAdapter = adapter; println("setter invoked")}

  @WebMethod
  def listStorages:Array[StorageDescription] = managementAdapter.listStorages

  @WebMethod
  def listStorageTypes:Array[String] = managementAdapter.listStorageTypes

  @WebMethod
  def createStorage(stType:String, path:String) = managementAdapter.createStorage(stType, path)

  @WebMethod
  def removeStorage(id:String) = managementAdapter.removeStorage(id)

  @WebMethod
  def migrate(source:String, target:String) = managementAdapter.migrate(source, target)

  @WebMethod
  def setStorageMode(id:String, mode:String) = managementAdapter.setStorageMode(id, mode)

  @WebMethod
  def setPlatformProperty(key:String, value:String) = managementAdapter.setPlatformProperty(key, value)

  @WebMethod
  def platformProperties:Array[Pair] = managementAdapter.platformProperties

  @WebMethod
  def listTasks:Array[TaskDescription] = managementAdapter.listTasks

  @WebMethod
  def listFinishedTasks:Array[TaskDescription] = managementAdapter.listFinishedTasks

  @WebMethod
  def setTaskMode(taskId:String, mode:String) = managementAdapter.setTaskMode(taskId, mode)

  @WebMethod
  def listTypeMappigs:Array[TypeMapping] = managementAdapter.listTypeMappigs

  @WebMethod
  def addTypeMapping(mimeType:String, storage:String, versioned:java.lang.Short)
    = managementAdapter.addTypeMapping(mimeType, storage, versioned)

  @WebMethod
  def removeTypeMapping(mimeType:String) = managementAdapter.removeTypeMapping(mimeType)

  @WebMethod
  def listSizeMappings:Array[SizeMapping] = managementAdapter.listSizeMappings

  @WebMethod
  def addSizeMapping(size:java.lang.Long, storage:String, versioned:java.lang.Integer)
    = managementAdapter.addSizeMapping(size, storage, versioned)

  @WebMethod
  def removeSizeMapping(size:java.lang.Long) = managementAdapter.removeSizeMapping(size)

  @WebMethod
  def listUsers:Array[Pair] = managementAdapter.listUsers

  @WebMethod
  def addUser(name:String, password:String, role:String) = managementAdapter.addUser(name, password, role)

  @WebMethod
  def updateUser(name:String, password:String, role:String) = managementAdapter.updateUser(name, password, role)

  @WebMethod
  def deleteUser(name:String) = managementAdapter.deleteUser(name)

  @WebMethod{val exclude=true}
  override def $tag:Int = super.$tag

}