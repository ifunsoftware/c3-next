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

import actors.AbstractActor
import actors.remote.RemoteActor._
import actors.remote.{RemoteActor, Node}
import javax.annotation.PreDestroy
import org.aphreet.c3.platform.access.AccessMediator
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.common.{Logger, WatchedActor}
import org.aphreet.c3.platform.domain.DomainManager
import org.aphreet.c3.platform.remote.replication._
import org.aphreet.c3.platform.remote.replication.impl.config.ConfigurationManager
import org.aphreet.c3.platform.remote.replication.impl.data.stats.DelayHistory
import org.aphreet.c3.platform.statistics.StatisticsManager
import org.aphreet.c3.platform.storage.StorageManager

class ReplicationAcceptorActor(val accessMediator: AccessMediator,
                               val storageManager: StorageManager,
                               val configurationManager: ConfigurationManager,
                               val domainManager: DomainManager,
                               val statisticsManager: StatisticsManager,
                               val sourceReplicationActor: ReplicationSenderActor) extends WatchedActor {

  val log = Logger(getClass)

  val WORKERS_COUNT = 8

  var delayHistory = new DelayHistory(statisticsManager)

  var replicationPort: Int = -1

  var config: Map[String, ReplicationHost] = Map()

  var remoteReplicationActors = Map[String, AbstractActor]()

  var workers = List[ReplicationTargetWorker]()

  var iterator: Iterator[ReplicationTargetWorker] = null

  var secureDataConnection = false

  var localSystemId: String = _

  def setUseSecureDataConnection(use: Boolean) {
    secureDataConnection = use
    workers.foreach(_.useSecureDataConnection = secureDataConnection)
  }

  def startWithConfig(config: Map[String, ReplicationHost], replicationPort: Int, localSystemId: String) {

    log info "Starting ReplicationTargetActor on port " + replicationPort

    this.localSystemId = localSystemId

    this.replicationPort = replicationPort

    this.config = config

    for (i <- 1 to WORKERS_COUNT) {
      val worker = new ReplicationTargetWorker(this.localSystemId,
        storageManager, accessMediator, configurationManager, domainManager, statisticsManager, delayHistory)

      worker.startWithConfig(this.config)
      worker.useSecureDataConnection = secureDataConnection
      workers = worker :: workers
    }

    iterator = workers.iterator

    this.start()

    log info "ReplicationTargetActor started"
  }

  def updateConfig(config: Map[String, ReplicationHost]) {
    this.config = config

    workers.foreach(_.updateConfig(this.config))
  }

  override def act() {

    alive(replicationPort)
    register('ReplicationActor, this)

    while (true) {
      receive {
        case DestroyMsg => {
          log info "DestoryMsg received. Stopping"

          workers.foreach(_ ! DestroyMsg)

          delayHistory.destroy()

          this.exit()
        }

        case ReplicateAddMsg(resource, signature) => {

          val target = getRemoteActor(signature.systemId)
          if (target != null)
            getNextWorker ! ProcessAddMsg(resource, signature, target)

        }

        case ReplicateUpdateMsg(resource, signature) => {

          val target = getRemoteActor(signature.systemId)
          if (target != null)
            getNextWorker ! ProcessUpdateMsg(resource, signature, target)

        }

        case ReplicateDeleteMsg(address, signature) => {
          val target = getRemoteActor(signature.systemId)
          if (target != null)
            getNextWorker ! ProcessDeleteMsg(address, signature, target)
        }

        case ReplicateSystemConfigMsg(configuration, signature) => {
          getNextWorker ! ReplicateSystemConfigMsg(configuration, signature)
        }

        case ReplicateAddAckMsg(address, signature) =>
          sourceReplicationActor ! ReplicateAddAckMsg(address, signature)

        case ReplicateUpdateAckMsg(address, timestamp, signature) =>
          sourceReplicationActor ! ReplicateUpdateAckMsg(address, timestamp, signature)

        case ReplicateDeleteAckMsg(address, signature) =>
          sourceReplicationActor ! ReplicateDeleteAckMsg(address, signature)
      }
    }
  }

  private def getNextWorker: ReplicationTargetWorker = {
    if (iterator.hasNext) {
      iterator.next()
    } else {
      iterator = workers.iterator
      iterator.next()
    }
  }


  private def getRemoteActor(id: String): AbstractActor = {

    remoteReplicationActors.get(id) match {
      case Some(a) => return a
      case None =>
    }

    log info "Creating remote actor for id " + id

    val host = config.get(id) match {
      case Some(h) => h
      case None => null
    }

    if (host != null) {

      val port = host.replicationPort.intValue

      val peer = Node(host.hostname, port)

      val remoteActor = RemoteActor.select(peer, 'ReplicationActor)

      remoteReplicationActors = remoteReplicationActors + ((id, remoteActor))

      remoteActor
    } else {
      log info "Can't find replication config for id " + id
      null
    }
  }

  @PreDestroy
  def destroy() {
    log info "Stopping ReplicationTarget Actor..."
    this ! DestroyMsg
  }
}