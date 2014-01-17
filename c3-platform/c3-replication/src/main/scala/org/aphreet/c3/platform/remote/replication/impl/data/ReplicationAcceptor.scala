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

import akka.actor.{Props, Actor, ActorRefFactory}
import org.aphreet.c3.platform.access.AccessMediator
import org.aphreet.c3.platform.common.{ActorRefHolder, Logger}
import org.aphreet.c3.platform.domain.DomainManager
import org.aphreet.c3.platform.remote.replication._
import org.aphreet.c3.platform.remote.replication.impl.ReplicationConstants.ActorInitialized
import org.aphreet.c3.platform.remote.replication.impl.config.ConfigurationManager
import org.aphreet.c3.platform.remote.replication.impl.data.stats.DelayHistory
import org.aphreet.c3.platform.statistics.StatisticsManager
import org.aphreet.c3.platform.storage.StorageManager

class ReplicationAcceptor(val actorSystem: ActorRefFactory,
                          val accessMediator: AccessMediator,
                          val storageManager: StorageManager,
                          val configurationManager: ConfigurationManager,
                          val domainManager: DomainManager,
                          val statisticsManager: StatisticsManager) extends ActorRefHolder {

  val log = Logger(getClass)

  val WORKERS_COUNT = 8

  var delayHistory = new DelayHistory(actorSystem, statisticsManager)

  var config: Map[String, ReplicationHost] = Map()

  var workers = List[ReplicationTargetWorker]()

  var iterator: Iterator[ReplicationTargetWorker] = null

  var secureDataConnection = false

  var localSystemId: String = _

  val async = actorSystem.actorOf(Props.create(classOf[ReplicationAcceptorActor], this), "ReplicationAcceptor")

  def setUseSecureDataConnection(use: Boolean) {
    secureDataConnection = use
    workers.foreach(_.useSecureDataConnection = secureDataConnection)
  }

  def startWithConfig(config: Map[String, ReplicationHost], localSystemId: String) {

    log info "Starting ReplicationAcceptor"

    this.localSystemId = localSystemId

    this.config = config

    for (i <- 1 to WORKERS_COUNT) {
      val worker = new ReplicationTargetWorker(actorSystem, this.localSystemId,
        storageManager, accessMediator, configurationManager, domainManager, statisticsManager, delayHistory)

      worker.startWithConfig(this.config)
      worker.useSecureDataConnection = secureDataConnection
      workers = worker :: workers
    }

    iterator = workers.iterator

    async ! ActorInitialized

    log info "ReplicationAcceptor started"
  }

  class ReplicationAcceptorActor extends Actor {

    def alive: Receive = {
      case ReplicateAddMsg(resource, signature) => {
        getNextWorker ! ProcessAddMsg(resource, signature, sender)
      }

      case ReplicateUpdateMsg(resource, signature) => {
        getNextWorker ! ProcessUpdateMsg(resource, signature, sender)
      }

      case ReplicateDeleteMsg(address, signature) => {
        getNextWorker ! ProcessDeleteMsg(address, signature, sender)
      }

      case ReplicateSystemConfigMsg(configuration, signature) => {
        getNextWorker ! ReplicateSystemConfigMsg(configuration, signature)
      }
    }

    def receive = {
      case ActorInitialized => context.become(alive)
    }
  }

  def updateConfig(config: Map[String, ReplicationHost]) {
    this.config = config

    workers.foreach(_.updateConfig(this.config))
  }


  private def getNextWorker: ReplicationTargetWorker = {
    if (iterator.hasNext) {
      iterator.next()
    } else {
      iterator = workers.iterator
      iterator.next()
    }
  }
}