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

import org.aphreet.c3.platform.access.ResourceAddedMsg
import org.aphreet.c3.platform.access.{AccessComponent, AccessMediator}
import org.aphreet.c3.platform.backup._
import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.config._
import org.aphreet.c3.platform.exception.StorageNotFoundException
import org.aphreet.c3.platform.filesystem.{FSComponent, FSManager}
import org.aphreet.c3.platform.query.{QueryComponent, QueryManager}
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.storage.{StorageComponent, StorageManager}
import org.aphreet.c3.platform.task._
import scala._

trait BackupComponentImpl extends BackupComponent {

  this: PlatformConfigComponent
    with StorageComponent
    with QueryComponent
    with AccessComponent
    with FSComponent
    with TaskComponent =>

  val backupManager = new BackupManagerImpl(new BackupConfigAccessor(configPersister))

  class BackupManagerImpl(val configAccessor: ConfigAccessor[List[BackupLocation]]) extends BackupManager with SPlatformPropertyListener with TaskScheduleObserver {

    val BACKUP_LOCATION = "c3.platform.backup.location"

    var targets: List[BackupLocation] = null

    val log = Logger(getClass)


    {
      log.info("Starting BackupManager")

      targets = configAccessor.load

      targets.foreach(target => target.schedule.foreach(s => scheduleBackup(target, s)))
    }

    def createBackup(targetId: String) {
      val target = getBackupLocation(targetId)

      val task = new BackupTask(queryManager, filesystemManager, target)

      taskManager.submitTask(task)
    }

    def restoreBackup(targetId: String, name: String) {
      val target = getBackupLocation(targetId)

      val backup = target.createBackup(name)

      storageManager.resetStorages()

      val task = new RestoreTask(storageManager, accessMediator, filesystemManager, backup)
      taskManager.submitTask(task)
    }

    def scheduleBackup(targetId: String, crontabSchedule: String) {
      val target = getBackupLocation(targetId)

      scheduleBackup(target, crontabSchedule)

      target.schedule ::= crontabSchedule
      configAccessor.update(l => targets)
    }

    private def scheduleBackup(target: BackupLocation, crontabSchedule: String) {
      val task = new BackupTask(queryManager, filesystemManager, target)

      taskManager.scheduleTask(task, crontabSchedule)

      task.addObserver(this)
    }

    def listBackups(targetId: String): List[String] = {
      getBackupLocation(targetId).listBackups
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

      val target = RemoteBackupLocation.create(id, host, 22, user, path, privateKeyFile, null)
      targets ::= target
      configAccessor.update(l => targets)
    }

    def removeTarget(id: String) {
      val target = getBackupLocation(id)
      targets = targets.diff(List(target))
      configAccessor.update(l => targets)
    }

    def listTargets(): List[BackupLocation] = targets

    def showTargetInfo(targetId: String): BackupLocation = getBackupLocation(targetId)

    def getBackupLocation(targetId: String): BackupLocation = {
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

    def updateSchedule(task: Task, newSchedule: String) {
      task match {
        case backupTask: BackupTask =>
          val target = getBackupLocation(backupTask.target.id)
          val oldSchedule = task.getSchedule

          target.schedule = target.schedule.diff(List(oldSchedule))
          if (newSchedule != null && !newSchedule.equals("")) {
            target.schedule ::= newSchedule
          }

          configAccessor.update(l => targets)
        case _ =>
      }
    }

    def existsTargetId(targetId: String): Boolean = {
      targets.find(target => target.id.equals(targetId)) match {

        case Some(value) => true
        case None => false
      }
    }

    def propertyChanged(event: PropertyChangeEvent) {}

    def defaultValues = Map(BACKUP_LOCATION -> System.getProperty("user.home"))
  }

  class BackupTask(val queryManager: QueryManager, val fsManager: FSManager, val target: BackupLocation)
    extends IteratorTask[Resource](() => queryManager.contentIterator(Map(), Map())) {

    var backup: AbstractBackup = null

    var backupName: String = null

    def processElement(element: Resource) {
      doBackup(element)
    }

    override def postFailure() {
      if (backup != null) {
        backup.close()
      }
    }

    override def preStart() {

      backupName = "backup-" + System.currentTimeMillis() + ".zip"

      log.info("Creating backup file " + backupName)

      backup = target.createBackup(backupName)
    }

    override def postComplete() {

      backup.writeFileSystemRoots(fsManager.fileSystemRoots)

      backup.close()

      log.info("Backup successfully completed")
    }

    protected def doBackup(resource: Resource) {
      if (log.isDebugEnabled) {
        log.debug("Adding resource " + resource.address + " to backup")
      }
      backup.addResource(resource)
    }
  }


  class RestoreTask(val storageManager: StorageManager, val accessMediator: AccessMediator,
                    val fsManager: FSManager, val backup: AbstractBackup)
    extends IterableTask[Resource](backup) {

    override def processElement(resource: Resource) {

      if (log.isDebugEnabled)
        log.debug("Importing resource " + resource.address)

      storageManager.storageForResource(resource) match {
        case Some(storage) => storage.update(resource)
        case None => throw new StorageNotFoundException("Can't find storage to import resource " + resource.address)
      }
      accessMediator ! ResourceAddedMsg(resource, 'BackupManager)
    }

    override protected def preStart() {
      for ((domain, address) <- backup.readFileSystemRoots) {
        try {
          fsManager.importFileSystemRoot(domain, address)
        } catch {
          case e: Throwable => log.warn("Failed to import file system root for " + domain + ": " + address, e)
        }
      }
    }

    override def postComplete() {
      if (backup != null) {
        backup.close()
      }
    }

    override def postFailure() {
      if (backup != null) {
        backup.close()
      }
    }
  }

}