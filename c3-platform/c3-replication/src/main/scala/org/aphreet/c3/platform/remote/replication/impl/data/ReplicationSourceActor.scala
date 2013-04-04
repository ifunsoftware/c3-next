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

import javax.annotation.PreDestroy
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.common.msg._
import org.aphreet.c3.platform.common.{Logger, ComponentGuard, WatchedActor}
import org.aphreet.c3.platform.remote.api.management.ReplicationHost
import org.aphreet.c3.platform.remote.replication._
import org.aphreet.c3.platform.remote.replication.impl.config.ConfigurationManager
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.statistics.StatisticsManager
import org.aphreet.c3.platform.storage.Storage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component


@Component
@Scope("singleton")
class ReplicationSourceActor extends WatchedActor with ComponentGuard{

  private var remoteReplicationActors = Map[String, ReplicationLink]()

  val log = Logger(getClass)

  var accessMediator:AccessMediator = _

  var manager:ReplicationManager = _

  var statisticsManager:StatisticsManager = _

  var configurationManager:ConfigurationManager = _

  var localSystemId:String = _

  @Autowired
  def setAccessMediator(mediator:AccessMediator) {accessMediator = mediator}

  @Autowired
  def setStatisticsManager(manager:StatisticsManager) {statisticsManager = manager}
  
  @Autowired
  def setConfigurationManager(manager:ConfigurationManager) {configurationManager = manager}


  def startWithConfig(config:Map[String, ReplicationHost], manager:ReplicationManager, localSystemId:String) {

    log info "Starting ReplicationSourceActor..."

    this.manager = manager
    this.localSystemId = localSystemId

    for((id, host) <- config) {
      remoteReplicationActors = remoteReplicationActors + ((id, new ReplicationLink(localSystemId, host, statisticsManager)))
    }

    accessMediator ! RegisterNamedListenerMsg(this, 'ReplicationManager)

    this.start()

    log info "ReplicationSourceActor started"
  }

  override def act(){
    loop{
      react{
        case ResourceAddedMsg(resource, source) => sendToAllLinks(ResourceAddedMsg(resource, source))

        case ResourceUpdatedMsg(resource, source) => sendToAllLinks(ResourceUpdatedMsg(resource, source))

        case ResourceDeletedMsg(address, source) if source != 'FSCleanupManager => sendToAllLinks(ResourceDeletedMsg(address, source))

        case StoragePurgedMsg(source) => manager ! StoragePurgedMsg(source)

        case ReplicateAddAckMsg(address, sign) => sendToLinkWithId(sign.systemId, ReplicateAddAckMsg(address, sign))

        case ReplicateUpdateAckMsg(address, timestamp, sign) => sendToLinkWithId(sign.systemId, ReplicateUpdateAckMsg(address, timestamp, sign))
          
        case ReplicateDeleteAckMsg(address, sign) => sendToLinkWithId(sign.systemId, ReplicateDeleteAckMsg(address, sign))

        case QueuedTasks => {
          log debug "Getting list of queued resources"
          sendToAllLinks(QueuedTasks)
        }

        case QueuedTasksReply(tasks) => {
          manager ! QueuedTasksReply(tasks)
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

        case DestroyMsg => {
          for((id, link) <- remoteReplicationActors){
            link.close()
          }

          remoteReplicationActors = Map()

          letItFall{
            accessMediator ! UnregisterNamedListenerMsg(this, 'ReplicationManager)
          }

          log info "ReplicationSourceActor stopped"
          this.exit()
        }
      }
    }
  }

  def addReplicationTarget(host:ReplicationHost) {
    remoteReplicationActors = remoteReplicationActors + ((host.systemId, new ReplicationLink(localSystemId, host, statisticsManager)))
  }

  def removeReplicationTarget(remoteSystemId:String) {
    val link = remoteReplicationActors.get(remoteSystemId).get

    remoteReplicationActors = remoteReplicationActors - remoteSystemId

    link.close()
  }

  def createCopyTasks(id:String, storageList:List[Storage]):Option[List[CopyTask]] = {

    remoteReplicationActors.get(id) match {
      case Some(replicationLink) => Some(storageList.map(s => new CopyTask(s, replicationLink)))
      case None => None
    }

  }

  private def sendToAllLinks(msg:Any) {
    try{
      for((id, link) <- remoteReplicationActors){
        if(!link.isStarted) link.start()
        link ! msg
      }
    }catch{
      case e: Throwable => log.error("Failed to post message: " + msg, e)
    }
  }

  private def sendToLinkWithId(id:String, msg:Any) {
    remoteReplicationActors.get(id) match {
      case Some(link) => {
        if(link.isStarted) link.start()
        link ! msg
      }
      case None => log.warn("Failed to send message, host does not exist: " + id + " msg: " + msg)
    }
  }

  @PreDestroy
  def destroy(){
    log info "Stopping ReplicationSourceActor..."
    this ! DestroyMsg
  }

}

case class ReplicationReplayAdd(resource:Resource, systemId:String)
case class ReplicationReplayDelete(address:String, systemId:String)
case class ReplicationReplayUpdate(resource:Resource, systemId:String)

object SendConfigurationMsg
case class SendConfigurationMsg(configuration:String)

