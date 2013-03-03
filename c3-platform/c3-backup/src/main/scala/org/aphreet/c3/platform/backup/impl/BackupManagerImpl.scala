/*
 * Copyright (c) 2012, Mikhail Malygin
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
 * 3. Neither the name of the iFunSoftware nor the names of its contributors
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

package org.aphreet.c3.platform.backup.impl

import org.aphreet.c3.platform.backup.{RemoteBackupLocation, LocalBackupLocation, BackupLocation, BackupManager}
import org.aphreet.c3.platform.storage.{StorageIterator, Storage, StorageManager}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.config.{PropertyChangeEvent, SPlatformPropertyListener, PlatformConfigManager}
import org.aphreet.c3.platform.task.{IterableTask, TaskManager, Task}
import org.aphreet.c3.platform.resource.{ResourceAddress, Resource}
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.access.{ResourceAddedMsg, AccessMediator}
import org.aphreet.c3.platform.filesystem.FSManager
import java.io.File
import collection.mutable.ListBuffer
import javax.annotation.PostConstruct
import org.apache.commons.logging.LogFactory
import org.slf4j.LoggerFactory

@Component("backupManager")
class BackupManagerImpl extends BackupManager with SPlatformPropertyListener{

  val BACKUP_LOCATION = "c3.platform.backup.location"

  @Autowired
  var storageManager:StorageManager = _

  @Autowired
  var accessMediator:AccessMediator = _

  @Autowired
  var filesystemManager:FSManager = _

  @Autowired
  var configManager:PlatformConfigManager = _

  @Autowired
  var taskManager:TaskManager = _

  @Autowired
  var configAccessor: BackupConfigAccessor = _

  var targets : List[BackupLocation] = null

  val log = LogFactory getLog getClass


  @PostConstruct
  def init(){
    targets = configAccessor.load
  }

  def createBackup(){
    val backupDirectory = configManager.getPlatformProperties.get(BACKUP_LOCATION) match {
      case Some(value) => value
      case None => throw new IllegalStateException("Can't create backup without " + BACKUP_LOCATION + " property")
    }

    val storages = storageManager.listStorages

    val task = new BackupTask(storages, filesystemManager, new Path(backupDirectory))

    taskManager.submitTask(task)
  }

  def restoreBackup(location:String){
    val backup = Backup.open(Path(location))

    storageManager.resetStorages()

    val task = new RestoreTask(storageManager, accessMediator, filesystemManager, backup)
    taskManager.submitTask(task)
  }

  def listBackups(targetId:String) : List[String] = {
    val target = getBackupLocation(targetId)

    target.backupType match {
      case "local" => listLocalBackups(target)
      case "remote" => listRemoteBackups(target)
      case _ => throw new IllegalStateException("Wrong target type")
    }
  }

  def listLocalBackups(target: BackupLocation) : List[String] = {
    val listBuffer = new ListBuffer[String]()
    val folder = new File(target.folder)

    if (folder.exists() && folder.isDirectory) {
      val filesList = folder.listFiles()

      filesList
        .filter( file => file.isFile && file.getName.endsWith(".zip") && Backup.hasValidChecksum(file.getAbsolutePath))
        .foreach( file => listBuffer += file.getAbsolutePath)
    }

    listBuffer.toList
  }

  def listRemoteBackups(target: BackupLocation) : List[String] = {
    val listBuffer = new ListBuffer[String]()

    val connector = new SftpConnector(target.host, target.user, target.privateKey)
    connector.connect()

    val allFilesNames = connector.listFiles(target.folder)

    log.info("All file Names in folder:")
    for (name <- allFilesNames) {
      log.info(name)
    }

    allFilesNames
      .filter( fileName => fileName.endsWith(".zip")
                             && !allFilesNames.find(str => str.equals(fileName + ".md5")).isEmpty
                             && RemoteBackup.hasValidChecksum(connector, target.folder, fileName))

      .foreach( fileName => listBuffer += fileName)

    connector.disconnect()

    log.info("Filtered file names in folder:")
    for (name <- listBuffer) {
      log.info(name)
    }

    listBuffer.toList
  }


  def createLocalTarget(id: String, path: String) {
    if (existsTargetId(id)) {
      throw new IllegalArgumentException("There is already a target with specified ID")
    }

    val target = LocalBackupLocation.create(id, path)
    targets ::= target
    configAccessor.update(l => targets)
  }

  def createRemoteTarget(id: String, host: String, user: String, path: String, privateKeyFile: String) {
    if (existsTargetId(id)) {
      throw new IllegalArgumentException("There is already a target with specified ID")
    }

    val target = RemoteBackupLocation.create(id, host, user, path, privateKeyFile)
    targets ::= target
    configAccessor.update(l => targets)
  }

  def removeTarget(id: String) {
    val target = getBackupLocation(id)
    targets = targets.diff(List(target))
    configAccessor.update(l => targets)
  }

  def listTargets() : List[String] = {
    val listBuffer = new ListBuffer[String]()
    var count = 1

    for (target <- targets) {
      val builder = new StringBuilder

      builder.append(count).append("\t")
      count += 1
      builder.append(target.id).append("\t")

      builder.append( target.backupType match {
        case "local" => "localhost"
        case "remote" => target.host
        case _ => throw new IllegalStateException("Wrong type for backup target")
      })
      builder.append("\t")

      builder.append(target.folder).append("\t")

      listBuffer += builder.toString()
    }

    listBuffer.toList
  }

  def showTargetInfo(targetId : String) : String = {
    val target = getBackupLocation(targetId)

    val builder = new StringBuilder("Target info\n")
    builder.append("ID: ").append(target.id).append("\n")

    val isRemote = target.backupType.equals("remote")
    builder.append("Type: ").append(target.backupType).append("\n")

    if (isRemote) {
      builder.append("Host: ").append(target.host).append("\n")
      builder.append("User: ").append(target.user).append("\n")
    }

    builder.append("Folder: ").append(target.folder).append("\n")

    if (isRemote) {
      builder.append("Key: ").append(target.privateKey).append("\n")
    }

    builder.toString()
  }

  def getBackupLocation(targetId : String) : BackupLocation = {
    val maybeLocation = targets.find(target => target.id.equals(targetId))

    val backupLocation = maybeLocation match {
      case Some(value) => value
      case None => {
        if (targetId.forall(_.isDigit)) {
          val num = targetId.toInt
          if (num >= 1 && num <= targets.size) {
            targets(num - 1)
          } else {
            throw new IllegalArgumentException("Target number is too big or less than 1")
          }
        } else {
          throw new IllegalArgumentException("Argument is not a target id nor target number")
        }
      }
    }

    backupLocation
  }

  def existsTargetId(targetId : String) : Boolean = {
    targets.find(target => target.id.equals(targetId)) match {

      case Some(value) => true
      case None => false
    }
  }

  def propertyChanged(event: PropertyChangeEvent) {}

  def defaultValues = Map(BACKUP_LOCATION -> System.getProperty("user.home"))
}

class BackupTask(val storages:List[Storage], val fsManager:FSManager, val directory:Path) extends Task{

  var iterator:StorageIterator = null

  var storagesToProcess = storages

  var backup:Backup = null

  var backupName:Path = null

  protected override def step() {
    if (iterator == null){
      storagesToProcess.headOption match {
        case Some(storage) => {

          log.info("Starting backup for storage " + storage.id)

          iterator = storage.iterator()
          storagesToProcess = storagesToProcess.tail
        }
        case None => shouldStopFlag = true
      }
    }else{
      if (iterator.hasNext){
        doBackup(iterator.next())
      }else{
        log.info("Backup for storage complete")
        iterator.close()
        iterator = null
      }
    }
  }

  override def postFailure(){
    if (iterator != null){
      iterator.close()
    }

    if (backup != null){
      backup.close()
    }

    if (backupName.file.exists()){
      backupName.file.delete()
    }
  }

  override def preStart(){

    if(!directory.file.exists()){
      directory.file.mkdirs()
    }

    backupName = directory.append("backup-" + System.currentTimeMillis() + ".zip")

    log.info("Creating backup file " + backupName.stringValue)

    backup = Backup.create(backupName)
  }

  override def postComplete(){

    backup.writeFileSystemRoots(fsManager.fileSystemRoots)

    backup.close()

    log.info("Backup successfully completed")
  }

  protected def doBackup(resource: Resource){
    if (log.isDebugEnabled){
      log.debug("Adding resource " + resource.address + " to backup")
    }
    backup.addResource(resource)
  }
}

class RestoreTask(val storageManager:StorageManager, val accessMediator:AccessMediator, val fsManager:FSManager, val backup:Backup)
  extends IterableTask[Resource](backup){

  override def processElement(resource:Resource){

    if(log.isDebugEnabled)
      log.debug("Importing resource " + resource.address)

    storageManager.storageForAddress(ResourceAddress(resource.address)).update(resource)
    accessMediator ! ResourceAddedMsg(resource, 'BackupManager)
  }

  override
  protected def preStart(){
    val fsRoots = backup.readFileSystemRoots

    for ((domain, address) <- fsRoots){
      try{
        fsManager.importFileSystemRoot(domain, address)
      }catch{
        case e:Throwable => log.warn("Failed to import file system root for " + domain + ": " + address, e)
      }
    }

  }
}
