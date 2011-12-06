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

import org.aphreet.c3.platform.task.{RUNNING, TaskState, PAUSED}
import org.aphreet.c3.platform.remote.api.management._
import org.aphreet.c3.platform.remote.api.RemoteException
import org.aphreet.c3.platform.auth.AuthenticationManager
import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.storage._
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.aphreet.c3.platform.exception.{PlatformException, StorageException}
import collection.JavaConversions._
import org.springframework.stereotype.Component
import javax.jws.{WebService}
import org.aphreet.c3.platform.remote.impl.PlatformManagementServiceUtil._
import org.springframework.web.context.support.SpringBeanAutowiringSupport
import org.springframework.web.context.ContextLoader
import org.aphreet.c3.platform.domain.DomainManager
import org.aphreet.c3.platform.filesystem.FSManager
import org.aphreet.c3.platform.remote.replication.ReplicationManager

@Component("platformManagementService")
@WebService(serviceName="ManagementService", targetNamespace="remote.c3.aphreet.org")
class PlatformManagementServiceImpl extends SpringBeanAutowiringSupport with PlatformManagementService{

  private var _managementEndpoint:PlatformManagementEndpoint = _

  private var _authenticationManager:AuthenticationManager = _

  private var _replicationManager:ReplicationManager = _

  private var _domainManager:DomainManager = _

  private var _filesystemManager:FSManager = _

  @Autowired
  private def setManagementEndpoint(endPoint:PlatformManagementEndpoint) = {
    _managementEndpoint = endPoint
  }

  @Autowired
  private def setAuthenticationManager(manager:AuthenticationManager) = {
    _authenticationManager = manager
  }

  @Autowired
  private def setReplicationManager(manager:ReplicationManager) = {
    _replicationManager = manager
  }

  @Autowired
  private def setDomainManager(manager:DomainManager) = {
    _domainManager = manager
  }

  @Autowired
  private def setFSManager(manager:FSManager) = {
    _filesystemManager = manager
  }

  private def managementEndpoint:PlatformManagementEndpoint = {
    if(_managementEndpoint == null){
      _managementEndpoint = ContextLoader.getCurrentWebApplicationContext.getBean("managementService", classOf[PlatformManagementEndpoint])
    }
    _managementEndpoint
  }

  private def authenticationManager:AuthenticationManager = {
    if(_authenticationManager == null){
      _authenticationManager = ContextLoader.getCurrentWebApplicationContext.getBean("authService", classOf[AuthenticationManager])
    }
    _authenticationManager
  }

  private def replicationManager:ReplicationManager = {
    if(_replicationManager == null){
      _replicationManager = ContextLoader.getCurrentWebApplicationContext.getBean("replicationService", classOf[ReplicationManager])
    }
    _replicationManager
  }

  private def domainManager:DomainManager = {
    if(_domainManager == null){
      _domainManager = ContextLoader.getCurrentWebApplicationContext.getBean("domainService", classOf[DomainManager])
    }
    _domainManager
  }

  private def filesystemManager:FSManager = {
    if(_filesystemManager == null){
      _filesystemManager = ContextLoader.getCurrentWebApplicationContext.getBean("filesystemService", classOf[FSManager])
    }
    _filesystemManager
  }

  def removeStorage(id:String) =
    try{
      managementEndpoint.removeStorage(id)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def listStorages:Array[StorageDescription] =
    try{
      managementEndpoint.listStorages.map(storageToDescription(_)).toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def listStorageTypes:Array[String] =
    try{
      managementEndpoint.listStorageTypes.toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }


  def createStorage(stType:String, path:String) =
    try{
      managementEndpoint.createStorage(stType, path)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def migrate(source:String, target:String) =
    try{
      managementEndpoint.migrateFromStorageToStorage(source, target)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def setStorageMode(id:String, mode:String) =
    try{
      val storageMode = mode match {
        case "RW" => RW(STORAGE_MODE_USER)
        case "RO" => RO(STORAGE_MODE_USER)
        case "U" => U(STORAGE_MODE_USER)
        case _ => throw new StorageException("No mode named " + mode)
      }
      managementEndpoint.setStorageMode(id, storageMode)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }


  def setPlatformProperty(key:String, value:String) =
    try{
      managementEndpoint.setPlatformProperty(key, value)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def platformProperties:Array[Pair] =
    try{
      (for(e <- asMap(managementEndpoint.getPlatformProperties))
      yield new Pair(e._1, e._2)).toSeq.toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def listTasks:Array[RemoteTaskDescription] =
    try{
      managementEndpoint.listTasks.map(fromLocalDescription(_)).toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def listFinishedTasks:Array[RemoteTaskDescription] =
    try{
      managementEndpoint.listFinishedTasks.map(fromLocalDescription(_)).toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }


  def setTaskMode(taskId:String, mode:String) =
    try{
      val state:TaskState = mode match {
        case "pause" => PAUSED
        case "resume"   => RUNNING
        case _ => throw new PlatformException("mode is not valid")

      }

      managementEndpoint.setTaskMode(taskId, state)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def listTypeMappings:Array[TypeMapping] =
    try{
      (for(entry <- managementEndpoint.listTypeMappings)
      yield new TypeMapping(entry._1, entry._2, entry._3)).toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def addTypeMapping(mimeType:String, storage:String, versioned:java.lang.Boolean) =
    try{
      managementEndpoint.addTypeMapping((mimeType, storage, versioned.booleanValue))
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def removeTypeMapping(mimeType:String) =
    try{
      managementEndpoint.removeTypeMapping(mimeType)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def listSizeMappings:Array[SizeMapping] =
    try{
      (for(entry <- managementEndpoint.listSizeMappings)
      yield new SizeMapping(entry._1, entry._2, entry._3)).toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def addSizeMapping(size:java.lang.Long, storage:String, versioned:java.lang.Boolean) =
    try{
      managementEndpoint.addSizeMapping((size.longValue, storage, versioned.booleanValue))
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def removeSizeMapping(size:java.lang.Long) =
    try{
      managementEndpoint.removeSizeMaping(size.longValue)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def listUsers:Array[UserDescription] =
    try{
      authenticationManager.list.map(e => new UserDescription(e.name, e.enabled)).toSeq.toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def addUser(name:String, password:String) = {
    try{
      authenticationManager.create(name, password)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def updateUser(name:String, password:String, enabled:java.lang.Boolean) = {
    try{
      authenticationManager.update(name, password, enabled.booleanValue)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def deleteUser(name:String) = {
    try{
      authenticationManager.delete(name)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def statistics:Array[Pair] = {
    try{
      (for((key,value) <- managementEndpoint.statistics)
      yield new Pair(key, value)).toSeq.toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def volumes:Array[VolumeDescription] = {
    try{
      (for(v <- managementEndpoint.listVolumes)
      yield new VolumeDescription(v.mountPoint, v.size, v.available, v.safeAvailable, v.storages.size)).toSeq.toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def createIndex(id:String, name:String, fields:Array[String], system:java.lang.Boolean, multi:java.lang.Boolean) = {
    try{
      val idx = new StorageIndex(name,
        fields.toList,
        multi.booleanValue,
        system.booleanValue,
        System.currentTimeMillis)

      managementEndpoint.createIndex(id, idx)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def removeIndex(id:String, name:String) = {
    try{
      managementEndpoint.removeIndex(id, name)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def addStorageSecondaryId(id:String, secId:String) = {
    try{
      managementEndpoint.addStorageSecondaryId(id, secId)
    }catch{
      case e=> {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def registerReplicationSource(host:ReplicationHost) = {
    try{
      replicationManager.registerReplicationSource(host)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def establishReplication(host:String, username:String, password:String) = {
    try{
      replicationManager.establishReplication(host, username, password)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def removeReplicationTarget(id:String) = {
    try{
      replicationManager.cancelReplication(id)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def listReplicationTargets:Array[ReplicationHost] = {
    try{
      replicationManager.listReplicationTargets
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def replayReplicationQueue = {
    try{
      replicationManager.replayReplicationQueue
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def createDomain(name:String) = {
    try{
      domainManager.addDomain(name)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def listDomains:Array[DomainDescription] = {
    try{
      (for(entry <- domainManager.domainList)
      yield new DomainDescription(entry.id, entry.name, entry.key, entry.mode.name)).toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def updateDomainName(name:String, newName:String) = {
    try{
      domainManager.updateName(name, newName)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def generateDomainKey(name:String):String = {
    try{
      domainManager.generateKey(name)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def setDomainMode(name:String, mode:String) = {
    try{
      domainManager.setMode(name, mode)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def listFileSystemRoots:Array[Pair] = {
    try{
      filesystemManager.fileSystemRoots.map(e => new Pair(e._1, e._2)).toSeq.toArray
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def importFileSystemRoot(domainId:String, address:String) = {
    try{
      filesystemManager.importFileSystemRoot(domainId, address)
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def startFilesystemCheck = {
    try{
      filesystemManager.startFilesystemCheck
    }catch{
      case e => {
        e.printStackTrace
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }
}
