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

import akka.actor.{Props, ActorRefFactory, Actor, ActorRef}
import collection.mutable
import encryption.DataEncryptor
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.common.{ActorRefHolder, Logger}
import org.aphreet.c3.platform.domain.{Domain, DomainManager}
import org.aphreet.c3.platform.exception.StorageNotFoundException
import org.aphreet.c3.platform.remote.replication._
import org.aphreet.c3.platform.remote.replication.impl.config._
import org.aphreet.c3.platform.resource.{ResourceAddress, Resource}
import org.aphreet.c3.platform.statistics.{IncreaseStatisticsMsg, StatisticsManager}
import org.aphreet.c3.platform.storage.StorageManager
import org.aphreet.c3.platform.remote.replication.impl.data.stats.{DelayHistory, DelayInfoMsg}

class ReplicationTargetWorker(val actorSystem: ActorRefFactory,
                              val localSystemId: String,
                              val storageManager: StorageManager,
                              val accessMediator: AccessMediator,
                              val configurationManager: ConfigurationManager,
                              val domainManager: DomainManager,
                              val statisticsManager: StatisticsManager,
                              val delayHistory: DelayHistory) extends ActorRefHolder {

  val log = Logger(getClass)

  var config: Map[String, ReplicationHost] = Map()

  var decryptors = new mutable.HashMap[String, DataEncryptor]()

  var useSecureDataConnection = false

  val async = actorSystem.actorOf(Props.create(classOf[ReplicationTargetWorkerActor], this))

  def startWithConfig(config: Map[String, ReplicationHost]) = {

    log info "Starting replication worker"

    updateConfig(config)
  }

  def updateConfig(config: Map[String, ReplicationHost]) {
    this.config = config

    val map = new mutable.HashMap[String, DataEncryptor]

    for ((id, host) <- config) {
      map.put(id, new DataEncryptor(host.encryptionKey))
    }

    decryptors = map
  }

  class ReplicationTargetWorkerActor extends Actor{

    def receive = {
      case ProcessAddMsg(bytes, sign, target) => replicateAdd(bytes, sign, target)

      case ProcessUpdateMsg(bytes, sign, target) => replicateUpdate(bytes, sign, target)

      case ProcessDeleteMsg(address, sign, target) => replicateDelete(address, sign, target)

      case ReplicateSystemConfigMsg(configuration, sign) => processConfiguration(configuration, sign)
    }
  }

  private def replicateAdd(bytes: Array[Byte], signature: ReplicationSignature, target: ActorRef) {

    log debug "Replicating incoming add"
    try {
      val host = checkSignature(bytes, signature)

      if (host == null) {
        log debug "Signature check failed"
        return
      }

      val decryptor = decryptors.get(signature.systemId).get

      val resource = Resource.fromByteArray(decryptor.decrypt(bytes))

      fillWithData(resource, host)

      storageManager.storageForResource(resource) match {
        case Some(storage) => {
          resource.verifyCheckSums()

          storage.update(resource)

          val calculator = new ReplicationSignatureCalculator(localSystemId, host)

          target ! ReplicateAddAckMsg(resource.address, calculator.calculate(resource.address))

          accessMediator ! ResourceAddedMsg(resource, 'ReplicationManager)

          statisticsManager ! IncreaseStatisticsMsg("c3.replication.created", 1)

          handleDelay(resource)
        }
        case None => throw new StorageNotFoundException("Can't find storage for resource " + resource.address)
      }
    } catch {
      case e: Throwable => {
        statisticsManager ! IncreaseStatisticsMsg("c3.replication.fail", 1)
        log.error("Failed to replicate add", e)
      }
    }
  }

  private def replicateUpdate(bytes: Array[Byte], signature: ReplicationSignature, target: ActorRef) {

    log debug "Replicating incoming update"

    try {
      val host = checkSignature(bytes, signature)

      if (host == null) {
        log debug "Signature check failed"
        return
      }

      val decryptor = decryptors.get(signature.systemId).get

      val resource = Resource.fromByteArray(decryptor.decrypt(bytes))

      storageManager.storageForResource(resource).filter(_.mode.allowWrite) match {
        case Some(storage) => {
          if (!storage.mode.allowWrite) {
            log warn "Failed to store resource, storage is not writable"
            return
          }

          fillWithData(resource, host)

          resource.verifyCheckSums()
          storage.update(resource)

          statisticsManager ! IncreaseStatisticsMsg("c3.replication.updated", 1)

          accessMediator ! ResourceUpdatedMsg(resource, 'ReplicationManager)

          handleDelay(resource)

          val calculator = new ReplicationSignatureCalculator(localSystemId, host)

          val timestamp: java.lang.Long = resource.lastUpdateDate.getTime

          target ! ReplicateUpdateAckMsg(resource.address, timestamp, calculator.calculate(resource.address))
        }
        case None => throw new StorageNotFoundException("Can't find storage for resource " + resource.address)
      }
    } catch {
      case e: Throwable => {
        statisticsManager ! IncreaseStatisticsMsg("c3.replication.fail", 1)
        log.error("Failed to replicate update", e)
      }
    }
  }

  private def replicateDelete(address: String, signature: ReplicationSignature, target: ActorRef) {
    try {

      val host = checkSignature(address.getBytes("UTF-8"), signature)

      if (host == null) {
        log debug "Signature check failed"
        return
      }

      storageManager.storageForAddress(ResourceAddress(address)).filter(_.mode.allowWrite) match {
        case Some(storage) => {

          storage.delete(address)

          target ! ReplicateDeleteAckMsg(address, new ReplicationSignatureCalculator(localSystemId, host).calculate(address))

          accessMediator ! ResourceDeletedMsg(address, 'ReplicationManager)

          statisticsManager ! IncreaseStatisticsMsg("c3.replication.deleted", 1)

        }
        case None => throw new StorageNotFoundException("Can't find storage for resource " + address)
      }

    } catch {
      case e: Throwable => {
        statisticsManager ! IncreaseStatisticsMsg("c3.replication.fail", 1)
        log.error("Failed to replicate update", e)
      }
    }
  }

  private def processConfiguration(configuration: String, signature: ReplicationSignature) {

    if (checkSignature(configuration, signature) != null) {
      log debug "Processing configuration"
      configurationManager.processSerializedRemoteConfiguration(configuration)
    } else {
      log info "Ignorring configuration due to incorrect message"
    }
  }

  private def fillWithData(resource: Resource, replicationHost: ReplicationHost) {

    val domainId = resource.systemMetadata(Domain.MD_FIELD).get

    val domain = domainManager.domainById(domainId) match {
      case Some(d) => d
      case None => throw new ReplicationException("Failed to replicate resource: " + resource.address + " due to unknown domain")
    }

    for (i <- 0 to resource.versions.size - 1) {
      val version = resource.versions(i)
      val data = new RemoteSystemDataStream(replicationHost, useSecureDataConnection, resource.address, i + 1, domain.id, domain.key)
      version.data = data
    }
  }

  private def handleDelay(resource: Resource){
    val timestamp = System.currentTimeMillis()

    val modificationTimestamp = resource.versions.last.date.getTime

    delayHistory ! DelayInfoMsg(timestamp, timestamp - modificationTimestamp)

  }

  private def checkSignature(bytes: Array[Byte], signature: ReplicationSignature): ReplicationHost = {

    ReplicationSignatureCalculator.foundAndVerify(bytes, signature, config) match {
      case Some(host) => host
      case None => null
    }
  }

  private def checkSignature(data: String, signature: ReplicationSignature): ReplicationHost = {

    ReplicationSignatureCalculator.foundAndVerify(data, signature, config) match {
      case Some(host) => host
      case None => null
    }
  }

}

case class ProcessAddMsg(
                          resource: Array[Byte],
                          signature: ReplicationSignature,
                          target: ActorRef
                          )

case class ProcessUpdateMsg(
                             resource: Array[Byte],
                             signature: ReplicationSignature,
                             target: ActorRef
                             )


case class ProcessDeleteMsg(
                             address: String,
                             signature: ReplicationSignature,
                             target: ActorRef
                             )