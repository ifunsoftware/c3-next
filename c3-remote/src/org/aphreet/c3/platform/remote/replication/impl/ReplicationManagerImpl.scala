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

import data._
import config._

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope

import javax.annotation.{PreDestroy, PostConstruct}

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.common.msg._
import org.aphreet.c3.platform.remote.HttpHost
import org.aphreet.c3.platform.remote.api.management._
import org.aphreet.c3.platform.remote.client.ManagementConnectionFactory
import org.aphreet.c3.platform.management.{PropertyChangeEvent, SPlatformPropertyListener}
import org.aphreet.c3.platform.exception.{PlatformException, ConfigurationException}
import org.aphreet.c3.platform.config.{UnregisterMsg, RegisterMsg, PlatformConfigManager}
import actors.remote.RemoteActor
import queue.{ReplicationQueueReplayTask, ReplicationQueueStorage}
import org.aphreet.c3.platform.storage.{StorageParams, StorageManager}
import org.aphreet.c3.platform.remote.replication.{ReplicationException, ReplicationManager}
import org.aphreet.c3.platform.task.TaskManager
import org.aphreet.c3.platform.common.{ComponentGuard, ThreadWatcher, Path, Constants}
import org.aphreet.c3.platform.resource.IdGenerator

@Component("replicationManager")
@Scope("singleton")
class ReplicationManagerImpl extends ReplicationManager with SPlatformPropertyListener with ComponentGuard{

  val HTTP_PORT_KEY = "c3.remote.http.port"
  val HTTPS_PORT_KEY = "c3.remote.https.port"
  val REPLICATION_PORT_KEY = "c3.remote.replication.port"
  val REPLICATION_QUEUE_KEY = "c3.remote.replication.queue"
  val REPLICATION_SECURE_KEY = "c3.remote.replication.secure_data"

  private val DEFAULT_REPLICATION_PORT = 7375

  val log = LogFactory getLog getClass


  var localReplicationActor:ReplicationTargetActor = null

  var sourceReplicationActor:ReplicationSourceActor = null

  var platformConfigManager:PlatformConfigManager = null

  var storageManager:StorageManager = null

  var taskManager:TaskManager = null

  var sourcesConfigAccessor:ReplicationSourcesConfigAccessor = null

  var targetsConfigAccessor:ReplicationTargetsConfigAccessor = null


  private var currentTargetConfig = Map[String, ReplicationHost]()

  private var currentSourceConfig = Map[String, ReplicationHost]()

  var replicationQueuePath:Path = null

  var replicationQueueStorage:ReplicationQueueStorage = null

  var isTaskRunning = false

  @PostConstruct
  def init{

    //Overriding classLoader
    RemoteActor.classLoader = getClass.getClassLoader


    log info "Starting replication manager..."

    val replicationPort = platformConfigManager.getPlatformProperties.get(REPLICATION_PORT_KEY) match{
      case Some(x) => x.toInt
      case None => DEFAULT_REPLICATION_PORT
    }

    val localSystemId = platformConfigManager.getPlatformProperties.get(Constants.C3_SYSTEM_ID) match{
      case Some(x) => x
      case None => throw new ConfigurationException("Local system ID is not found")
    }

    currentSourceConfig = sourcesConfigAccessor.load
    localReplicationActor.startWithConfig(currentSourceConfig, replicationPort, localSystemId)

    currentTargetConfig = targetsConfigAccessor.load
    sourceReplicationActor.startWithConfig(currentTargetConfig, this, localSystemId)

    runQueueMaintainer

    this.start

    platformConfigManager ! RegisterMsg(this)

    log info "Replicationg manager started"
  }

  @PreDestroy
  def destroy{
    log info "Destroying ReplicationManager"

    this ! DestroyMsg
  }

  override def act{
    loop{
      react{
        case QueuedTasks => {
          log debug "Getting list of queued resources"
          sourceReplicationActor ! QueuedTasks
        }

        case QueuedTasksReply(tasks) => {
          log debug "Got list of queued resources"

          if(replicationQueueStorage != null){

            replicationQueueStorage.add(tasks)


          }else{
            log warn "Replication queue path is not set. Queue will be lost!"
          }
        }

        case SendConfigurationMsg => {
          log debug "Sending configuration to targets"
          sourceReplicationActor ! SendConfigurationMsg
        }

        case DestroyMsg => {

          letItFall{
            platformConfigManager ! UnregisterMsg(this)
          }

          log info "RemoteManagerActor stopped"
          this.exit

        }
      }
    }
  }

  def listFailedReplicationQueues:Array[String] = {
    if(replicationQueuePath != null){

      val file = replicationQueuePath.file

      if(file.isDirectory){
        file.list
      }else{
        throw new ConfigurationException("Replication queue path is file")
      }

    }else{
      throw new ConfigurationException("Replication queue path is not set")
    }
  }

  def showFailedReplicationQueue(index:Int):Array[String] = {
    throw new PlatformException("Is not implemented yet")
  }

  def retryFailedReplicationQueue(index:Int) = {
    throw new PlatformException("Is not implemented yet")
  }

  def listReplicationTargets:Array[ReplicationHost] = {
    currentTargetConfig.values.toArray
  }

  def getReplicationTarget(systemId:String):ReplicationHost = {
    currentTargetConfig.get(systemId) match {
      case Some(host) => host
      case None => null
    }
  }

  /**
   * First check that all storage ids from this machine exist on remote machine
   *
   * After that, generate replication key
   *
   * And write remote source config and local target
   */
  def establishReplication(host:String, user:String, password:String) = {

    val managementService = getManagementService(host, user, password)

    val secret = IdGenerator.generateId(5)

    val localReplicationHost = createLocalReplicationHost(secret)

    val remoteReplicationHost = createRemoteReplicationHost(managementService, secret)

    syncStorageConfigurations(managementService)

    managementService.registerReplicationSource(localReplicationHost)
    this.registerReplicationTarget(remoteReplicationHost)
  }

  def cancelReplication(id:String) = {
    targetsConfigAccessor.update(config => config - id)
    currentTargetConfig = targetsConfigAccessor.load

    sourceReplicationActor.removeReplicationTarget(id)
  }

  def registerReplicationSource(host:ReplicationHost) = {
    sourcesConfigAccessor.update(config => config + ((host.systemId, host)))

    currentSourceConfig = sourcesConfigAccessor.load
    localReplicationActor.updateConfig(currentSourceConfig)

    log info "Registered replication source " + host.hostname + " with id " + host.systemId
  }

  private def registerReplicationTarget(host:ReplicationHost) = {

    targetsConfigAccessor.update(config => config + ((host.systemId, host)))
    currentTargetConfig = targetsConfigAccessor.load

    sourceReplicationActor.addReplicationTarget(host)

    log info "Registered replication target " + host.hostname + " with id " + host.systemId
  }

  private def syncStorageConfigurations(managementService:PlatformManagementService) = {
    val remoteStorages = managementService.listStorages.toList
    val localStorages = storageManager.listStorages

    log info "Comparing storage configuration..."

    val additionalIds = new StorageSynchronizer().getAdditionalIds(remoteStorages, localStorages)

    log info "Done"

    log info "Applying configuration changes..."

    for((id, secId) <- additionalIds){
      managementService.addStorageSecondaryId(id, secId)
    }

    log info "Done"
  }

  private def createLocalReplicationHost(secret:String):ReplicationHost = {
    createReplicationHost(createLocalPropertyRetriever, secret)
  }

  private def createRemoteReplicationHost(service:PlatformManagementService, secret:String):ReplicationHost = {

    val platformProperties = service.platformProperties

    createReplicationHost(createRemotePropertyRetriever(platformProperties), secret)
  }

  private def createReplicationHost(propertyRetriever:Function1[String, String], secret:String):ReplicationHost = {

    val systemId = propertyRetriever(Constants.C3_SYSTEM_ID)
    val systemHost = propertyRetriever(Constants.C3_PUBLIC_HOSTNAME)
    val httpPort = propertyRetriever(HTTP_PORT_KEY).toInt
    val httpsPort = propertyRetriever(HTTPS_PORT_KEY).toInt
    val replicationPort = propertyRetriever(REPLICATION_PORT_KEY).toInt
    val secretKey = secret

    ReplicationHost(systemId, systemHost, secretKey, httpPort, httpsPort, replicationPort)
  }

  private def createLocalPropertyRetriever:Function1[String, String] = {
    ((key:String) => platformConfigManager.getPlatformProperties.get(key) match {
      case Some(value) => value
      case None => throw new ConfigurationException("Failed to get property " + key)
    })
  }

  private def createRemotePropertyRetriever(remoteProperties:Array[Pair]):Function1[String, String] = {
    ((key:String) => {
      val options = remoteProperties.filter(_.key == key)
      if(options.isEmpty){
        throw new ConfigurationException("Failed to get remote system id")
      }

      options.head.value
    })
  }

  private def getManagementService(host:String, user:String, password:String):PlatformManagementService = {
    log info "Connecting to remote system..."
    val service = ManagementConnectionFactory.connect(HttpHost(host, user, password))
    log info "Done"
    service
  }

  private def runQueueMaintainer = {
    val thread = new Thread(new ProcessScheduler(this))
    thread.setDaemon(true)
    thread.start
  }

  override def defaultValues:Map[String, String] =
    Map(HTTP_PORT_KEY -> "7373",
      HTTPS_PORT_KEY -> "7374",
      REPLICATION_PORT_KEY -> DEFAULT_REPLICATION_PORT.toString,
      REPLICATION_QUEUE_KEY -> "",
      REPLICATION_SECURE_KEY -> "false")

  override def propertyChanged(event:PropertyChangeEvent) = {

    event.name match {
      case REPLICATION_QUEUE_KEY =>
        if(event.newValue.isEmpty){
          replicationQueuePath = null
        }else{
          replicationQueuePath = new Path(event.newValue)
          if(replicationQueueStorage != null){
            replicationQueueStorage.close
          }
          replicationQueueStorage = new ReplicationQueueStorage(replicationQueuePath)
        }

      case REPLICATION_SECURE_KEY =>
        localReplicationActor.setUseSecureDataConnection(event.newValue == "true")

      case _ =>
        log warn "To get new port config working system restart is required"
    }
  }

  override def replayReplicationQueue = {
    this.synchronized{
      if(isTaskRunning) throw new ReplicationException("Task already started")
      if(replicationQueueStorage != null){
        val task = new ReplicationQueueReplayTask(this, storageManager, replicationQueueStorage, sourceReplicationActor)

        taskManager.submitTask(task)
        isTaskRunning = true
        log info "Replay task submitted"
      }else{
        throw new ReplicationException("Replication queue storage is not initialized")
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------//

  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}

  @Autowired
  def setSourcesConfigAccessor(accessor:ReplicationSourcesConfigAccessor) = {sourcesConfigAccessor = accessor}

  @Autowired
  def setTargetsConfigAccessor(accessor:ReplicationTargetsConfigAccessor) = {targetsConfigAccessor = accessor}

  @Autowired
  def setConfigManager(manager:PlatformConfigManager) = {platformConfigManager = manager}

  @Autowired
  def setTaskManager(manager:TaskManager) = {taskManager = manager}

  @Autowired
  def setLocalReplicationActor(actor:ReplicationTargetActor) = {localReplicationActor = actor}

  @Autowired
  def setSourceReplicationActor(actor:ReplicationSourceActor) = {sourceReplicationActor = actor}
}

class ProcessScheduler(manager:ReplicationManager) extends Runnable{

  val log = LogFactory getLog getClass

  val FIVE_MINUTES = 1000 * 60 * 5

  var nextConfigSendTime = System.currentTimeMillis + FIVE_MINUTES

  var nextQueueProcessTime = System.currentTimeMillis + FIVE_MINUTES

  override def run{

    ThreadWatcher + this
    try{
      log info "Starting replication background process scheduler"

      while(!Thread.currentThread.isInterrupted){

        while(!Thread.currentThread.isInterrupted){
          triggerConfigExchange
          triggerQueueProcess

          Thread.sleep(60 * 1000)
        }
      }
    }finally{
      ThreadWatcher - this
      log info "Stopping Replication background process scheduler"
    }
  }

  def triggerConfigExchange{

    if(nextConfigSendTime - System.currentTimeMillis < 0){
      log debug "Sending configuration to targets"

      manager ! SendConfigurationMsg

      nextConfigSendTime = System.currentTimeMillis + FIVE_MINUTES
      
    }

  }

  def triggerQueueProcess{

    if(nextQueueProcessTime - System.currentTimeMillis < 0){
      log debug "Getting replication queue"

      manager ! QueuedTasks

      nextQueueProcessTime = System.currentTimeMillis + FIVE_MINUTES
    }
  }
}
