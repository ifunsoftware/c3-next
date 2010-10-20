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

  def addTypeMapping(mimeType:String, storage:String, versioned:java.lang.Boolean)

  def removeTypeMapping(mimeType:String)

  def listSizeMappings:Array[SizeMapping]

  def addSizeMapping(size:java.lang.Long, storage:String, versioned:java.lang.Boolean)

  def removeSizeMapping(size:java.lang.Long)

  def listUsers:Array[UserDescription]

  def addUser(name:String, password:String, role:String)

  def updateUser(name:String, password:String, role:String, enabled:java.lang.Boolean)

  def deleteUser(name:String)

  def statistics:Array[Pair]

  def volumes:Array[VolumeDescription]

  def createIndex(id:String, name:String, fields:Array[String], system:java.lang.Boolean, multi:java.lang.Boolean)

  def removeIndex(id:String, name:String)

  def addStorageSecondaryId(id:String, secId:String)
  
  def registerReplicationSource(host:ReplicationHost)


}