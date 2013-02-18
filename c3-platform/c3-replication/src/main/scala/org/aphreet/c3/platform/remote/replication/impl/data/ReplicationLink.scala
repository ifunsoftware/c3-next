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

import encryption.DataEncryptor
import org.aphreet.c3.platform.remote.api.management.ReplicationHost
import org.apache.commons.logging.LogFactory
import actors.remote.{RemoteActor, Node}
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.access.{ResourceUpdatedMsg, ResourceDeletedMsg, ResourceAddedMsg}
import org.aphreet.c3.platform.remote.replication._
import collection.mutable.{HashSet, HashMap}
import org.aphreet.c3.platform.statistics.{IncreaseStatisticsMsg, StatisticsManager}
import actors.AbstractActor
import org.aphreet.c3.platform.common.WatchedActor

class ReplicationLink(val localSystemId:String,
                      val host:ReplicationHost,
                      val statisticsManager:StatisticsManager) extends WatchedActor{

  val log = LogFactory getLog getClass

  def isStarted:Boolean = started

  private var started = false

  val replicationTimeout = 1000 * 60 * 5

  private val queue = new HashMap[ReplicationTask, Long]

  private val dataEncryptor = new DataEncryptor(host.encryptionKey)

  override def act(){

    val calculator = new ReplicationSignatureCalculator(localSystemId, host)

    log info "Establishing replication link to " + host.systemId

    val port = host.replicationPort.intValue

    val peer = Node(host.hostname, port)

    val remoteActor = RemoteActor.select(peer, 'ReplicationActor)

    link(remoteActor)

    started = true

    loop{
      react{

        case ResourceAddedMsg(resource, source) => {
          val bytes = resource.toByteArray

          val encrypted = dataEncryptor.encrypt(bytes)

          sendRemoteMessage(remoteActor, ReplicateAddMsg(encrypted, calculator.calculate(encrypted)))

          statisticsManager ! IncreaseStatisticsMsg("c3.replication.submit.add." + host.systemId, 1l)

          if(log.isTraceEnabled)
            log trace "Adding RAE to queue " + resource.address

          queue += ((ReplicationTask(host.systemId, resource.address, AddAction), System.currentTimeMillis))
        }

        case ReplicateAddAckMsg(address, signature) => {
          if(calculator.verify(address, signature)){
            queue -= ReplicationTask(host.systemId, address, AddAction)

            statisticsManager ! IncreaseStatisticsMsg("c3.replication.ack.add." + host.systemId, 1l)

            if(log.isTraceEnabled)
              log trace "Removing RAE from queue " + address
          }
        }

        case ResourceUpdatedMsg(resource, source) => {
          val bytes = resource.toByteArray

          val encrypted = dataEncryptor.encrypt(bytes)

          sendRemoteMessage(remoteActor, ReplicateUpdateMsg(encrypted, calculator.calculate(encrypted)))

          statisticsManager ! IncreaseStatisticsMsg("c3.replication.submit.update." + host.systemId, 1l)

          val timestamp:java.lang.Long = resource.lastUpdateDate.getTime

          if(log.isTraceEnabled)
            log trace "Adding RUE to queue " + resource.address

          queue += ((ReplicationTask(host.systemId, resource.address, UpdateAction(timestamp.longValue)), System.currentTimeMillis))

        }

        case ReplicateUpdateAckMsg(address, timestamp, signature) => {
          if(calculator.verify(address, signature)){

            statisticsManager ! IncreaseStatisticsMsg("c3.replication.ack.update." + host.systemId, 1l)

            if(log.isTraceEnabled)
              log trace "Removing RUE from queue " + address

            queue -= ReplicationTask(host.systemId, address, UpdateAction(timestamp.longValue))
          }
        }



        case ResourceDeletedMsg(address, source) => {
          sendRemoteMessage(remoteActor, ReplicateDeleteMsg(address, calculator.calculate(address)))

          statisticsManager ! IncreaseStatisticsMsg("c3.replication.submit.delete." + host.systemId, 1l)

          if(log.isTraceEnabled)
            log trace "Adding RDE to queue " + address

          queue += ((ReplicationTask(host.systemId, address, DeleteAction), System.currentTimeMillis))
        }

        case ReplicateDeleteAckMsg(address, signature) => {
          if(calculator.verify(address, signature)){

            statisticsManager ! IncreaseStatisticsMsg("c3.replication.ack.delete." + host.systemId, 1l)

            if(log.isTraceEnabled)
              log trace "Removing RDE from queue " + address

            queue -= ReplicationTask(host.systemId, address, DeleteAction)
          }
        }

        case QueuedTasks => {
          log debug "Retrieving queued tasks"

          val set = new HashSet[ReplicationTask]

          set ++= queue.filter(System.currentTimeMillis.longValue - _._2.longValue > replicationTimeout).map(e => e._1)

          if(!set.isEmpty){

            queue --= set

            if(log.isTraceEnabled)
              log.trace("Returning queue: " + set.toString)

            statisticsManager ! IncreaseStatisticsMsg("c3.replication.queued." + host.systemId, set.size)

            sender ! QueuedTasksReply(set)
          }else{
            if(log.isTraceEnabled)
              log.trace("Replication queue is empty for id " + host.systemId) 
          }
        }

        case SendConfigurationMsg(configuration) => {
          sendRemoteMessage(remoteActor, ReplicateSystemConfigMsg(configuration, calculator.calculate(configuration)))

          statisticsManager ! IncreaseStatisticsMsg("c3.replication.sendconfig." + host.systemId, 1l)
        }

        case DestroyMsg => {
          log info "Destroying replication link to " + host.toString
          unlink(remoteActor)
          this.exit()
        }
      }
    }
  }

  private def sendRemoteMessage(actor:AbstractActor, message:Any){
    try{
      actor ! message
    }catch{
      case e: Throwable => log error ("Failed to send message ", e)
    }
  }

  def close(){
    log info "Closing replication link"
    this ! DestroyMsg
  }
}

object QueuedTasks
case class QueuedTasksReply(set:HashSet[ReplicationTask])
