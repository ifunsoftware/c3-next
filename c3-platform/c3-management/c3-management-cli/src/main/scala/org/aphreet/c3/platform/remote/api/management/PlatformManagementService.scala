package org.aphreet.c3.platform.remote.api.management

trait PlatformManagementService {

  def removeStorage(id:String)

  def listStorages:Array[StorageDescription]

  def listStorageTypes:Array[String]

  def createStorage(stType:String, path:String)

  def purgeStorageData()

  def migrateStorage(source: String, target: String)

  def setStorageMode(id:String, mode:String)

  def createStorageIndex(name: String, fields: Array[String], system: java.lang.Boolean, multi: java.lang.Boolean)

  def removeStorageIndex(name: String)

//Implemented in rest interface
  def setPlatformProperty(key:String, value:String)

  def platformProperties:Array[Pair]

//Implemented in rest interface
  def listTasks:Array[RemoteTaskDescription]

  def listFinishedTasks:Array[RemoteTaskDescription]

  def setTaskMode(taskId:String, mode:String)

  def listScheduledTasks:Array[RemoteTaskDescription]

  def rescheduleTask(id: String, crontabSchedule: String)

  def removeScheduledTask(id: String)


  def listTypeMappings:Array[TypeMapping]

  def addTypeMapping(mimeType:String, versioned:java.lang.Boolean)

  def removeTypeMapping(mimeType:String)


//Implemented in rest interface
  def listUsers:Array[UserDescription]

  def addUser(name:String, password:String)

  def updateUser(name:String, password:String, enabled:java.lang.Boolean)

  def deleteUser(name:String)


//Implemented in the rest interface
  def statistics:Array[Pair]


  def createReplicationTarget(host:String, port:java.lang.Integer, username:String, password:String)

  def removeReplicationTarget(id:String)

  def listReplicationTargets:Array[ReplicationHostDescription]

  def replayReplicationQueue()

  def copyDataToReplicationTarget(id:String)

  def resetReplicationQueue()

  def dumpReplicationQueue(path: String)

//Implemented in the rest interface
  def createDomain(name:String)

  def listDomains:Array[DomainDescription]

  def updateDomainName(name:String, newName:String)

  def generateDomainKey(name:String):String

  def setDomainMode(name:String, mode:String)

  def setDefaultDomain(domainId: String)

  def removeDomainKey(name: String)

  def getDefaultDomain: String

  def deleteDomain(name: String)


  def listFilesystemRoots:Array[Pair]

  def importFilesystemRoot(domainId:String, address:String)

  def startFilesystemCheck()


  def resetSearchIndex()

  def dropSearchIndex()

  def dumpSearchIndex(path: String)


  def createBackup(targetId : String)

  def restoreBackup(targetId: String, name: String)

  def scheduleBackup(targetId: String, crontabSchedule: String)

  def listBackups(targetId : String) : Array[String]

  def createLocalBackupTarget(id : String, path : String)

  def createRemoteBackupTarget(id : String, host : String, user : String, path : String, privateKeyFile : String)

  def removeBackupTarget(id : String)

  def listBackupTargets() : Array[TargetDescription]

  def showBackupTargetInfo(targetId: String) : TargetDescription

}