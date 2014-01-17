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
package org.aphreet.c3.platform.remote.replication.impl

import akka.actor.ActorRefFactory
import config._
import data._
import java.io.File
import org.aphreet.c3.platform.access.AccessMediator
import org.aphreet.c3.platform.common._
import org.aphreet.c3.platform.config._
import org.aphreet.c3.platform.domain.DomainManager
import org.aphreet.c3.platform.exception.{PlatformException, ConfigurationException}
import org.aphreet.c3.platform.remote.replication.ReplicationHost
import org.aphreet.c3.platform.remote.replication.impl.ReplicationConstants._
import org.aphreet.c3.platform.remote.replication.impl.data.queue._
import org.aphreet.c3.platform.remote.replication.{ReplicationException, ReplicationManager}
import org.aphreet.c3.platform.statistics.StatisticsManager
import org.aphreet.c3.platform.storage.StorageManager
import org.aphreet.c3.platform.task.TaskManager
import scala.Some

class ReplicationManagerImpl(val actorSystem: ActorRefFactory,
                             val accessMediator: AccessMediator,
                             val storageManager: StorageManager,
                             val taskManager: TaskManager,
                             val domainManager: DomainManager,
                             val statisticsManager: StatisticsManager,
                             val platformConfigManager: PlatformConfigManager,
                             val configPersister: ConfigPersister,
                             val configurationManager: ConfigurationManager) extends ReplicationManager with SPlatformPropertyListener with ComponentGuard {

  val log = Logger(getClass)

  var localSystemId: String = ""

  private var currentTargetConfig = Map[String, ReplicationHost]()

  private var currentSourceConfig = Map[String, ReplicationHost]()

  val replicationQueueStorageHolder = new ReplicationQueueStorageHolderImpl

  var isTaskRunning = false

  var replicationSender = new ReplicationSender(actorSystem, accessMediator, statisticsManager,
    configurationManager, replicationQueueStorageHolder)

  val replicationAcceptor = new ReplicationAcceptor(actorSystem, accessMediator,
    storageManager, configurationManager, domainManager, statisticsManager)


  val sourcesConfigAccessor = new ReplicationSourcesConfigAccessor(configPersister)

  val targetsConfigAccessor = new ReplicationTargetsConfigAccessor(configPersister)

  {
    log info "Starting replication manager..."

    localSystemId = platformConfigManager.getPlatformProperties.get(Constants.C3_SYSTEM_ID) match {
      case Some(x) => x
      case None => throw new ConfigurationException("Local system ID is not found")
    }

    currentSourceConfig = sourcesConfigAccessor.load
    replicationAcceptor.startWithConfig(currentSourceConfig, localSystemId)

    currentTargetConfig = targetsConfigAccessor.load
    replicationSender.startWithConfig(currentTargetConfig, localSystemId)

    platformConfigManager ! RegisterMsg(this)

    log info "Replicationg manager started"
  }

  def destroy() {
    log info "Destroying ReplicationManager"

    letItFall {
      platformConfigManager ! UnregisterMsg(this)
    }
  }

  def listFailedReplicationQueues: Array[String] = {
    Array()
  }

  def showFailedReplicationQueue(index: Int): Array[String] = {
    throw new PlatformException("Is not implemented yet")
  }

  def retryFailedReplicationQueue(index: Int) {
    throw new PlatformException("Is not implemented yet")
  }

  def listReplicationTargets: Array[ReplicationHost] = {
    currentTargetConfig.values.toArray
  }

  def getReplicationTarget(systemId: String): ReplicationHost = {
    currentTargetConfig.get(systemId) match {
      case Some(host) => host
      case None => null
    }
  }

  def establishReplication(host: String, port: Int, user: String, password: String) {
    val replicationHost = new ReplicationNegotiatorClient(actorSystem, localSystemId, configurationManager)
      .establishReplication(host, port, user, password)

    registerReplicationTarget(replicationHost)
  }

  def copyToTarget(targetId: String) {

    log.info("Creating tasks for copying data to target " + targetId)

    replicationSender
      .createCopyTasks(targetId, storageManager.listStorages) match {
      case Some(tasks) => tasks.foreach(taskManager.submitTask(_))
      case None => throw new ReplicationException("Can't find target with id " + targetId)
    }
  }

  def cancelReplication(id: String) {
    targetsConfigAccessor.update(config => config - id)
    currentTargetConfig = targetsConfigAccessor.load

    replicationSender.removeReplicationTarget(id)
  }

  def registerReplicationSource(host: ReplicationHost) {
    sourcesConfigAccessor.update(config => config + ((host.systemId, host)))

    currentSourceConfig = sourcesConfigAccessor.load
    replicationAcceptor.updateConfig(currentSourceConfig)

    log info "Registered replication source " + host.hostname + " with id " + host.systemId
  }

  private def registerReplicationTarget(host: ReplicationHost) {

    targetsConfigAccessor.update(config => config + ((host.systemId, host)))
    currentTargetConfig = targetsConfigAccessor.load

    replicationSender.addReplicationTarget(host)

    log info "Registered replication target " + host.hostname + " with id " + host.systemId
  }

  override def defaultValues: Map[String, String] =
    Map(HTTP_PORT_KEY -> "7373",
      HTTPS_PORT_KEY -> "7374",
      REPLICATION_QUEUE_KEY -> new File(platformConfigManager.dataDir, "queue").getAbsolutePath,
      REPLICATION_SECURE_KEY -> "false"
    )

  override def propertyChanged(event: PropertyChangeEvent) {

    event.name match {
      case REPLICATION_QUEUE_KEY =>
        if (event.newValue.isEmpty) {
          replicationQueueStorageHolder.updateStoragePath(None)
        } else {
          replicationQueueStorageHolder.updateStoragePath(Some(Path(event.newValue)))
        }

      case REPLICATION_SECURE_KEY =>
        replicationAcceptor.setUseSecureDataConnection(event.newValue == "true")

      case _ =>
        log warn "To get new port config working system restart is required"
    }
  }


  override def replayReplicationQueue() {
    this.synchronized {
      if (isTaskRunning) throw new ReplicationException("Task already started")

      replicationQueueStorageHolder.storage.map{storage =>
        val task = new ReplicationQueueReplayTask(this, storageManager, storage, replicationSender.async)

        taskManager.submitTask(task)
        isTaskRunning = true
        log info "Replay task submitted"
      }
    }
  }

  override def resetReplicationQueue() {
    this.synchronized {
      log.info("Clearing replication queue")
      replicationQueueStorageHolder.storage.map(_.clear())
    }
  }

  def dumpReplicationQueue(path: String) {
    replicationQueueStorageHolder.storage.map{
      storage => log.info("Dumping replication queue to path: " + path)
        taskManager.submitTask(new ReplicationQueueDumpTask(storage, Path(path)))
    }
  }
}


