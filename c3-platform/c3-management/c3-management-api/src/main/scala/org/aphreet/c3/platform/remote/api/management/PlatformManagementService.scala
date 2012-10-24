package org.aphreet.c3.platform.remote.api.management

import javax.jws.WebService


@WebService(serviceName="ManagementService", targetNamespace="remote.c3.aphreet.org")
trait PlatformManagementService {

  def removeStorage(id:String)

  def listStorages:Array[StorageDescription]

  def listStorageTypes:Array[String]

  def createStorage(stType:String, path:String)

  def migrate(source:String, target:String)

  def setStorageMode(id:String, mode:String)

  def setPlatformProperty(key:String, value:String)

  def platformProperties:Array[Pair]

  def listTasks:Array[RemoteTaskDescription]

  def listFinishedTasks:Array[RemoteTaskDescription]

  def setTaskMode(taskId:String, mode:String)


  def listTypeMappings:Array[TypeMapping]

  def addTypeMapping(mimeType:String, versioned:java.lang.Boolean)

  def removeTypeMapping(mimeType:String)


  def listUsers:Array[UserDescription]

  def addUser(name:String, password:String)

  def updateUser(name:String, password:String, enabled:java.lang.Boolean)

  def deleteUser(name:String)

  def statistics:Array[Pair]

  def volumes:Array[VolumeDescription]

  def createIndex(id:String, name:String, fields:Array[String], system:java.lang.Boolean, multi:java.lang.Boolean)

  def removeIndex(id:String, name:String)

  def establishReplication(host:String, username:String, password:String)

  def removeReplicationTarget(id:String)

  def listReplicationTargets:Array[ReplicationHost]

  def replayReplicationQueue()


  def createDomain(name:String)

  def listDomains:Array[DomainDescription]

  def updateDomainName(name:String, newName:String)

  def generateDomainKey(name:String):String

  def setDomainMode(name:String, mode:String)

  def listFileSystemRoots:Array[Pair]

  def importFileSystemRoot(domainId:String, address:String)

  def startFilesystemCheck()

  def resetSearchIndex()

  def createBackup()

  def restoreBackup(location:String)

}