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

import collection.JavaConversions._
import org.aphreet.c3.platform.auth.AuthenticationManager
import org.aphreet.c3.platform.backup.BackupManager
import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.domain.DomainManager
import org.aphreet.c3.platform.exception.{PlatformException, StorageException}
import org.aphreet.c3.platform.filesystem.FSManager
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.aphreet.c3.platform.remote.api.RemoteException
import org.aphreet.c3.platform.remote.api.management._
import org.aphreet.c3.platform.remote.impl.PlatformManagementServiceUtil._
import org.aphreet.c3.platform.remote.replication.ReplicationManager
import org.aphreet.c3.platform.search.SearchManager
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.task.{RUNNING, TaskState, PAUSED}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component("platformManagementService")
class PlatformManagementServiceImpl extends PlatformManagementService{

  @Autowired
  var managementEndpoint:PlatformManagementEndpoint = _

  @Autowired
  var authenticationManager:AuthenticationManager = _

  @Autowired
  var replicationManager:ReplicationManager = _

  @Autowired
  var domainManager:DomainManager = _

  @Autowired
  var filesystemManager:FSManager = _

  @Autowired
  var backupManager:BackupManager = _

  @Autowired
  var searchManager: SearchManager = _

  def removeStorage(id:String) {
    try {
      managementEndpoint.removeStorage(id)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def listStorages:Array[StorageDescription] =
    try{
      managementEndpoint.listStorages.map(storageToDescription(_)).toArray
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def listStorageTypes:Array[String] =
    try{
      managementEndpoint.listStorageTypes.toArray
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def purgeStorageData(){
    try{
      managementEndpoint.purgeStorageData()
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def createStorage(stType:String, path:String) {
    try {
      managementEndpoint.createStorage(stType, path)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def migrateStorage(source:String, target:String) {
    try {
      managementEndpoint.migrateFromStorageToStorage(source, target)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def setStorageMode(id:String, mode:String) {
    try {
      val storageMode = mode match {
        case "RW" => RW(STORAGE_MODE_USER)
        case "RO" => RO(STORAGE_MODE_USER)
        case "U" => U(STORAGE_MODE_USER)
        case _ => throw new StorageException("No mode named " + mode)
      }
      managementEndpoint.setStorageMode(id, storageMode)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }


  def setPlatformProperty(key:String, value:String) {
    try {
      managementEndpoint.setPlatformProperty(key, value)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def platformProperties:Array[Pair] =
    try{
      (for(e <- mapAsScalaMap(managementEndpoint.getPlatformProperties))
      yield new Pair(e._1, e._2)).toSeq.toArray
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def listTasks:Array[RemoteTaskDescription] =
    try{
      managementEndpoint.listTasks.map(fromLocalDescription(_)).toArray
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def listFinishedTasks:Array[RemoteTaskDescription] =
    try{
      managementEndpoint.listFinishedTasks.map(fromLocalDescription(_)).toArray
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }


  def setTaskMode(taskId:String, mode:String) {
    try {
      val state: TaskState = mode match {
        case "pause" => PAUSED
        case "resume" => RUNNING
        case _ => throw new PlatformException("mode is not valid")

      }

      managementEndpoint.setTaskMode(taskId, state)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def listScheduledTasks = {
    try {
      managementEndpoint.listScheduledTasks.map(fromLocalDescription(_)).toArray
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def rescheduleTask(id: String, crontabSchedule: String) {
    try {
      managementEndpoint.rescheduleTask(id, crontabSchedule)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def removeScheduledTask(id: String) {
    try {
      managementEndpoint.removeScheduledTask(id)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def listTypeMappings:Array[TypeMapping] =
    try{
      (for(entry <- managementEndpoint.listTypeMappings)
      yield new TypeMapping(entry._1, entry._2)).toArray
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def addTypeMapping(mimeType:String, versioned:java.lang.Boolean) {
    try {
      managementEndpoint.addTypeMapping((mimeType, versioned.booleanValue))
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def removeTypeMapping(mimeType:String) {
    try {
      managementEndpoint.removeTypeMapping(mimeType)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def listUsers:Array[UserDescription] =
    try{
      authenticationManager.list.map(e => new UserDescription(e.name, e.enabled)).toSeq.toArray
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }

  def addUser(name:String, password:String) {
    try{
      authenticationManager.create(name, password)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def updateUser(name:String, password:String, enabled:java.lang.Boolean) {
    try{
      authenticationManager.update(name, password, enabled.booleanValue)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def deleteUser(name:String) {
    try{
      authenticationManager.delete(name)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def statistics:Array[Pair] = {
    try{
      (for((key,value) <- managementEndpoint.statistics)
      yield new Pair(key, value)).toSeq.toArray
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def createStorageIndex(name:String, fields:Array[String], system:java.lang.Boolean, multi:java.lang.Boolean) {
    try{
      val idx = new StorageIndex(name,
        fields.toList,
        multi.booleanValue,
        system.booleanValue,
        System.currentTimeMillis)

      managementEndpoint.createIndex(idx)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def removeStorageIndex(name:String) {
    try{
      managementEndpoint.removeIndex(name)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def createReplicationTarget(host:String, port:java.lang.Integer, username:String, password:String) {
    try{
      replicationManager.establishReplication(host, port, username, password)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def removeReplicationTarget(id:String) {
    try{
      replicationManager.cancelReplication(id)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def listReplicationTargets:Array[ReplicationHostDescription] = {
    try{
      replicationManager.listReplicationTargets.map(h =>
        new ReplicationHostDescription(h.systemId, h.hostname, h.key, h.httpPort, h.httpsPort, h.replicationPort, h.encryptionKey))
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def replayReplicationQueue() {
    try{
      replicationManager.replayReplicationQueue()
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }


  def resetReplicationQueue() {
    try{
      replicationManager.resetReplicationQueue()
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def copyDataToReplicationTarget(id:String) {
    try{
      replicationManager.copyToTarget(id)
    }catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def dumpReplicationQueue(path: String) {
    try{
      replicationManager.dumpReplicationQueue(path)
    }catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def createDomain(name:String) {
    try{
      domainManager.addDomain(name)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def listDomains:Array[DomainDescription] = {
    try{
      (for(entry <- domainManager.domainList)
      yield new DomainDescription(entry.id, entry.name, entry.key, entry.mode.name, entry.deleted)).toArray
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def updateDomainName(name:String, newName:String) {
    try{
      domainManager.updateName(name, newName)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def generateDomainKey(name:String):String = {
    try{
      domainManager.generateKey(name)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def setDomainMode(name:String, mode:String) {
    try{
      domainManager.setMode(name, mode)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }


  def setDefaultDomain(domainId: String) {
    try{
      domainManager.setDefaultDomain(domainId)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def removeDomainKey(name: String) {
    try{
      domainManager.removeKey(name)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }


  def getDefaultDomain = {
    try{
      domainManager.getDefaultDomainId
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def deleteDomain(name: String) {
    try{
      domainManager.deleteDomain(name)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def listFilesystemRoots:Array[Pair] = {
    try{
      filesystemManager.fileSystemRoots.map(e => new Pair(e._1, e._2)).toSeq.toArray
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def importFilesystemRoot(domainId:String, address:String) {
    try{
      filesystemManager.importFileSystemRoot(domainId, address)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def startFilesystemCheck(){
    try{
      filesystemManager.startFilesystemCheck()
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def createBackup(targetId: String){
    try{
      backupManager.createBackup(targetId)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def restoreBackup(targetId: String, name: String){
    try{
      backupManager.restoreBackup(targetId, name)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def scheduleBackup(targetId: String, crontabSchedule: String) {
    try{
      backupManager.scheduleBackup(targetId, crontabSchedule)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def resetSearchIndex() {

    try{
      managementEndpoint.setPlatformProperty("c3.search.index.create_timestamp", System.currentTimeMillis().toString)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def dropSearchIndex() {
    try{
      searchManager.deleteIndexes()
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }


  def listBackups(targetId : String) : Array[String] = {
    try {
      backupManager.listBackups(targetId).toArray
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }


  def createLocalBackupTarget(id: String, path: String) {

    try {
      backupManager.createLocalTarget(id, path)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def createRemoteBackupTarget(id: String, host: String, user: String, path: String, privateKeyFile: String) {

    try {
      backupManager.createRemoteTarget(id, host, user, path, privateKeyFile)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def removeBackupTarget(id: String) {
    try {
      backupManager.removeTarget(id)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def listBackupTargets() : Array[TargetDescription] = {
    try {
      backupManager.listTargets()
        .map(e => new TargetDescription(e.id, e.backupType, e.host, e.user, e.folder, e.privateKey))
        .toArray
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def showBackupTargetInfo(targetId: String) : TargetDescription = {
    try {
      val target = backupManager.showTargetInfo(targetId)
      val targetDescription = new TargetDescription(target.id, target.backupType, target.host, target.user,
        target.folder, target.privateKey)

      targetDescription
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }

  def dumpSearchIndex(path: String) {
    try{
      searchManager.dumpIndex(path)
    }catch{
      case e: Throwable => {
        e.printStackTrace()
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }
}