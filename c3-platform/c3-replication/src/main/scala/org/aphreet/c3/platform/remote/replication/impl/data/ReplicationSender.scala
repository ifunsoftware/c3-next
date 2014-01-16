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
package org.aphreet.c3.platform.remote.replication.impl.data

import akka.actor._
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.common.msg._
import org.aphreet.c3.platform.common.{ActorRefHolder, Logger, ComponentGuard}
import org.aphreet.c3.platform.remote.replication.ReplicationHost
import org.aphreet.c3.platform.remote.replication.impl.config.ConfigurationManager
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.statistics.StatisticsManager
import org.aphreet.c3.platform.storage.Storage
import scala.Some
import org.aphreet.c3.platform.remote.replication.impl.data.queue.ReplicationQueueStorageHolder
import org.aphreet.c3.platform.remote.replication.impl.ReplicationConstants.ActorInitialized

class ReplicationSender(val actorSystem: ActorRefFactory,
                         val accessMediator: AccessMediator,
                              val statisticsManager: StatisticsManager,
                              val configurationManager: ConfigurationManager,
                              val replicationQueueStorageHolder: ReplicationQueueStorageHolder) extends ActorRefHolder with ComponentGuard{

  private var remoteReplicationActors = Map[String, ActorRef]()

  val log = Logger(getClass)

  var localSystemId:String = _

  val async = actorSystem.actorOf(Props.create(classOf[ReplicationSenderActor], this))

  def startWithConfig(config:Map[String, ReplicationHost], localSystemId:String) {

    log info "Starting ReplicationSourceActor..."

    this.localSystemId = localSystemId

    for((id, host) <- config) {
      remoteReplicationActors = remoteReplicationActors + ((id, createReplicationLink(id, host, statisticsManager)))
    }

    accessMediator ! RegisterNamedListenerMsg(async, 'ReplicationManager)

    new ReplicationMaintainThread().start()

    log info "ReplicationSourceActor started"
  }
  
  private def createReplicationLink(systemId: String, host: ReplicationHost, statisticsManager: StatisticsManager): ActorRef = {
    actorSystem.actorOf(Props.create(classOf[ReplicationLink], systemId, host, statisticsManager))
  }

  class ReplicationSenderActor extends Actor{

    def initialized: Receive = {
      case ResourceAddedMsg(resource, source) => sendToAllLinks(ResourceAddedMsg(resource, source))

      case ResourceUpdatedMsg(resource, source) => sendToAllLinks(ResourceUpdatedMsg(resource, source))

      case ResourceDeletedMsg(address, source) if source != 'FSCleanupManager => sendToAllLinks(ResourceDeletedMsg(address, source))

      case StoragePurgedMsg(source) => replicationQueueStorageHolder.storage.map(_.clear())

      case QueuedTasks => {
        log debug "Getting list of queued resources"
        sendToAllLinks(QueuedTasks)
      }

      case QueuedTasksReply(tasks) => {
        replicationQueueStorageHolder.storage.map(_.add(tasks))
      }

      case ReplicationReplayAdd(resource, systemId) => sendToLinkWithId(systemId, ResourceAddedMsg(resource, 'ReplicationManager))

      case ReplicationReplayUpdate(resource, systemId) => sendToLinkWithId(systemId, ResourceUpdatedMsg(resource, 'ReplicationManager))

      case ReplicationReplayDelete(address, systemId) => sendToLinkWithId(systemId, ResourceDeletedMsg(address, 'ReplicationManager))

      case SendConfigurationMsg => {

        log debug "Retrieving configuration from manager"

        val configuration = configurationManager.getSerializedConfiguration

        log debug "Got configuration, distributing over targets"

        sendToAllLinks(SendConfigurationMsg(configuration))
      }
    }

    def receive = {
      case ActorInitialized => context.become(initialized)
    }

    override def postStop(){
      accessMediator ! UnregisterNamedListenerMsg(self, 'ReplicationManager)
    }
  }

  def addReplicationTarget(host:ReplicationHost) {
    remoteReplicationActors = remoteReplicationActors + ((host.systemId, createReplicationLink(localSystemId, host, statisticsManager)))
  }

  def removeReplicationTarget(remoteSystemId:String) {
    val link = remoteReplicationActors.get(remoteSystemId).get

    remoteReplicationActors = remoteReplicationActors - remoteSystemId

    link ! PoisonPill
  }

  def createCopyTasks(id:String, storageList:List[Storage]):Option[List[CopyTask]] = {

    remoteReplicationActors.get(id) match {
      case Some(replicationLink) => Some(storageList.map(s => new CopyTask(s, replicationLink, localSystemId)))
      case None => None
    }

  }

  private def sendToAllLinks(msg:Any) {
    try{
      for((id, link) <- remoteReplicationActors){

        link ! msg
      }
    }catch{
      case e: Throwable => log.error("Failed to post message: " + msg, e)
    }
  }

  private def sendToLinkWithId(id:String, msg:Any) {
    remoteReplicationActors.get(id) match {
      case Some(link) => {
        link ! msg
      }
      case None => log.warn("Failed to send message, host does not exist: " + id + " msg: " + msg)
    }
  }

  class ReplicationMaintainThread extends Thread {

    val log = Logger(getClass)

    {
      setDaemon(true)
    }

    override def run() {

      log.info("Starting replication maintain thread")

      while (!isInterrupted) {

        Thread.sleep(5 * 60 * 1000l)

        triggerConfigExchange()
        triggerQueueProcess()
      }

      log.info("Replication maintain thread has completed")
    }

    def triggerConfigExchange() {
      log debug "Sending configuration to targets"

      async ! SendConfigurationMsg
    }

    def triggerQueueProcess() {
      log debug "Getting replication queue"

      async ! QueuedTasks
    }
  }

}

case class ReplicationReplayAdd(resource:Resource, systemId:String)
case class ReplicationReplayDelete(address:String, systemId:String)
case class ReplicationReplayUpdate(resource:Resource, systemId:String)

object SendConfigurationMsg
case class SendConfigurationMsg(configuration:String)

