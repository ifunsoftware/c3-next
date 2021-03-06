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

import akka.actor.{Terminated, ActorRef, Actor}
import akka.util.Timeout
import encryption.DataEncryptor
import java.util.concurrent.TimeUnit
import org.aphreet.c3.platform.access.{ResourceUpdatedMsg, ResourceDeletedMsg, ResourceAddedMsg}
import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.remote.replication._
import org.aphreet.c3.platform.remote.replication.impl.ReplicationConstants.ActorInitialized
import org.aphreet.c3.platform.statistics.{IncreaseStatisticsMsg, StatisticsManager}
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class ReplicationLink(val localSystemId: String,
                      val host: ReplicationHost,
                      val statisticsManager: StatisticsManager) extends Actor {

  implicit val timeout = Timeout(1, TimeUnit.MINUTES)

  val log = Logger(getClass)

  val replicationTimeout = 1000 * 60 * 5

  private val queue = new mutable.HashMap[ReplicationTask, Long]

  private val dataEncryptor = new DataEncryptor(host.encryptionKey)

  private val calculator = new ReplicationSignatureCalculator(localSystemId, host)

  private var remoteActor: ActorRef = _

  import ExecutionContext.Implicits.global

  override def preStart() {
    log info "Establishing replication link to " + host.systemId
  }

  override def postStop() {
    if (remoteActor != null) {
      context.unwatch(remoteActor)
    }
  }

  private def resolveReplicationAcceptor(host: ReplicationHost) = {
    val selection = context.actorSelection(s"akka.tcp://c3-replication@${host.hostname}:${host.replicationPort}/user/ReplicationAcceptor")
    val acceptorFuture = selection.resolveOne(timeout.duration)

    acceptorFuture.onComplete {
      case Success(acceptor) => {
        remoteActor = acceptor
        log.info("Got remote actor, becoming alive")
        context.become(alive)
        context.watch(remoteActor)
      }
      case Failure(t) => {
        log.info("Failed to resolve remote actor", t.getMessage)
        context.system.scheduler.scheduleOnce(5 seconds, self, ResolveAcceptor(host))
      }
    }
  }

  def alive: Receive = {

    case Terminated(remote) =>
      log.info("Got terminated message for remote actor, starting wait for reconnection")
      context.unwatch(remoteActor)
      context.become(waitForConnection)

      self ! ResolveAcceptor(host)

    case ResourceAddedMsg(resource, source) => {
      val bytes = resource.toByteArray

      val encrypted = dataEncryptor.encrypt(bytes)

      remoteActor ! ReplicateAddMsg(encrypted, calculator.calculate(encrypted))

      statisticsManager ! IncreaseStatisticsMsg("c3.replication.submit.add." + host.systemId, 1l)

      if (log.isTraceEnabled)
        log trace "Adding RAE to queue " + resource.address

      queue += ((ReplicationTask(host.systemId, resource.address, AddAction), System.currentTimeMillis))
    }

    case ReplicateAddAckMsg(address, signature) => {
      if (calculator.verify(address, signature)) {
        queue -= ReplicationTask(host.systemId, address, AddAction)

        statisticsManager ! IncreaseStatisticsMsg("c3.replication.ack.add." + host.systemId, 1l)

        if (log.isTraceEnabled)
          log trace "Removing RAE from queue " + address
      }
    }

    case ResourceUpdatedMsg(resource, source) => {
      val bytes = resource.toByteArray

      val encrypted = dataEncryptor.encrypt(bytes)

      remoteActor ! ReplicateUpdateMsg(encrypted, calculator.calculate(encrypted))

      statisticsManager ! IncreaseStatisticsMsg("c3.replication.submit.update." + host.systemId, 1l)

      val timestamp: java.lang.Long = resource.lastUpdateDate.getTime

      if (log.isTraceEnabled)
        log trace "Adding RUE to queue " + resource.address

      queue += ((ReplicationTask(host.systemId, resource.address, UpdateAction(timestamp.longValue)), System.currentTimeMillis))

    }

    case ReplicateUpdateAckMsg(address, timestamp, signature) => {
      if (calculator.verify(address, signature)) {

        statisticsManager ! IncreaseStatisticsMsg("c3.replication.ack.update." + host.systemId, 1l)

        if (log.isTraceEnabled)
          log trace "Removing RUE from queue " + address

        queue -= ReplicationTask(host.systemId, address, UpdateAction(timestamp.longValue))
      }
    }


    case ResourceDeletedMsg(address, source) => {
      remoteActor ! ReplicateDeleteMsg(address, calculator.calculate(address))

      statisticsManager ! IncreaseStatisticsMsg("c3.replication.submit.delete." + host.systemId, 1l)

      if (log.isTraceEnabled)
        log trace "Adding RDE to queue " + address

      queue += ((ReplicationTask(host.systemId, address, DeleteAction), System.currentTimeMillis))
    }

    case ReplicateDeleteAckMsg(address, signature) => {
      if (calculator.verify(address, signature)) {

        statisticsManager ! IncreaseStatisticsMsg("c3.replication.ack.delete." + host.systemId, 1l)

        if (log.isTraceEnabled)
          log trace "Removing RDE from queue " + address

        queue -= ReplicationTask(host.systemId, address, DeleteAction)
      }
    }

    case QueuedTasks => sendQueuedTasks(sender)

    case SendConfigurationMsg(configuration) => {
      remoteActor ! ReplicateSystemConfigMsg(configuration, calculator.calculate(configuration))

      statisticsManager ! IncreaseStatisticsMsg("c3.replication.sendconfig." + host.systemId, 1l)
    }
  }

  def waitForConnection: Receive = {
    case ResolveAcceptor(replicationHost) => resolveReplicationAcceptor(host)

    case ResourceAddedMsg(resource, source) => {
      persistTasks(sender, List(ReplicationTask(host.systemId, resource.address, AddAction)))
    }


    case ResourceUpdatedMsg(resource, source) => {
      val timestamp = resource.lastUpdateDate.getTime

      persistTasks(sender, List(ReplicationTask(host.systemId, resource.address, UpdateAction(timestamp.longValue))))
    }

    case ResourceDeletedMsg(address, source) => {
      persistTasks(sender, List(ReplicationTask(host.systemId, address, DeleteAction)))
    }

    case ActorInitialized => {
      self ! ResolveAcceptor(host)
    }

    case QueuedTasks => sendQueuedTasks(sender)

    case SendConfigurationMsg(configuration) =>
  }

  def receive = waitForConnection

  private def sendQueuedTasks(sender: ActorRef) {
    log debug "Retrieving queued tasks"

    val set = new mutable.HashSet[ReplicationTask]

    set ++= queue.filter(System.currentTimeMillis.longValue - _._2.longValue > replicationTimeout).map(e => e._1)

    if (!set.isEmpty) {

      queue --= set

      if (log.isTraceEnabled)
        log.trace("Returning queue: " + set.toString)

      persistTasks(sender, set)

    } else {
      if (log.isTraceEnabled)
        log.trace("Replication queue is empty for id " + host.systemId)
    }
  }

  private def persistTasks(sender: ActorRef, tasks: Iterable[ReplicationTask]) {
    statisticsManager ! IncreaseStatisticsMsg("c3.replication.queued." + host.systemId, tasks.size)

    sender ! QueuedTasksReply(tasks)
  }

}

case class ResolveAcceptor(host: ReplicationHost)

object QueuedTasks

case class QueuedTasksReply(tasks: Iterable[ReplicationTask])
