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
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.resource.{AddressGenerator, Resource}
import org.aphreet.c3.platform.storage.StorageManager
import actors.{AbstractActor, Actor}
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.remote.replication._
import org.aphreet.c3.platform.remote.replication.impl.config._
import org.aphreet.c3.platform.common.WatchedActor
import org.aphreet.c3.platform.domain.{Domain, DomainManager}
import collection.mutable.HashMap

class ReplicationTargetWorker(val localSystemId:String,
                              val storageManager:StorageManager,
                              val accessMediator:AccessMediator,
                              val configurationManager:ConfigurationManager,
                              val domainManager:DomainManager) extends WatchedActor {

  val log = LogFactory getLog getClass

  var config:Map[String, ReplicationHost] = Map()

  var decryptors:HashMap[String, DataEncryptor] = new HashMap()

  var useSecureDataConnection = false

  def startWithConfig(config:Map[String, ReplicationHost]) = {

    log info "Starting replication worker"

    updateConfig(config)

    this.start
  }

  def updateConfig(config:Map[String, ReplicationHost]) = {
    this.config = config

    val map = new HashMap[String, DataEncryptor]

    for((id, host) <- config){
      map.put(id, new DataEncryptor(host.encryptionKey))
    }

    decryptors = map
  }

  override def act{
    loop{
      react{

        case ProcessAddMsg(bytes, sign, target) => replicateAdd(bytes, sign, target)

        case ProcessUpdateMsg(bytes, sign, target) => replicateUpdate(bytes, sign, target)

        case ProcessDeleteMsg(address, sign, target) => replicateDelete(address, sign, target)

        case ReplicateSystemConfigMsg(configuration, sign) => processConfiguration(configuration, sign)

        case DestroyMsg => {
          log info "Stopping replication worker"
          this.exit
        }
      }
    }
  }

  private def replicateAdd(bytes:Array[Byte], signature:ReplicationSignature, target:AbstractActor){

    log debug "Replicating incoming add"
    try{
      val host = checkSignature(bytes, signature)

      if(host == null){
        log debug "Signature check failed"
        return
      }

      val decryptor = decryptors.get(signature.systemId).get

      val resource = Resource.fromByteArray(decryptor.decrypt(bytes))

      fillWithData(resource, host)

      val storageId = AddressGenerator.storageForAddress(resource.address)

      val storage = storageManager.storageForId(storageId)

      if(!storage.mode.allowWrite){
        log debug "Failed to replicate resource, storage is not writtable"
        return
      }


      resource.verifyCheckSums
      storage.put(resource)

      val calculator = new ReplicationSignatureCalculator(localSystemId, host)

      target ! ReplicateAddAckMsg(resource.address, calculator.calculate(resource.address))

      accessMediator ! ResourceAddedMsg(resource, 'ReplicationManager)

    }catch{
      case e => log.error("Failed to replicate add", e)
    }
  }

  private def replicateUpdate(bytes:Array[Byte], signature:ReplicationSignature, target:AbstractActor){

    log debug "Replicating incoming update"

    try{
      val host = checkSignature(bytes, signature)

      if(host == null){
        log debug "Signature check failed"
        return
      }

      val decryptor = decryptors.get(signature.systemId).get

      val resource = Resource.fromByteArray(decryptor.decrypt(bytes))

      val storageId = AddressGenerator.storageForAddress(resource.address)
      
      val storage = storageManager.storageForId(storageId)

      if(!storage.mode.allowWrite){
        log warn "Failed to store resource, storage is not writable"
        return
      }

      fillWithData(resource, host)


      storage.get(resource.address) match{
        case Some(r) => {
          compareUpdatedResource(resource, r)
          resource.verifyCheckSums
          storage.update(resource)

          accessMediator ! ResourceUpdatedMsg(resource, 'ReplicationManager)

        }
        case None => {
          resource.verifyCheckSums
          storage.put(resource)

          accessMediator ! ResourceAddedMsg(resource, 'ReplicationManager)
        }
      }

      val calculator = new ReplicationSignatureCalculator(localSystemId, host)

      val timestamp:java.lang.Long = resource.lastUpdateDate.getTime

      target ! ReplicateUpdateAckMsg(resource.address, timestamp, calculator.calculate(resource.address))

    }catch{
      case e => log.error("Failed to replicate update", e)
    }
  }

  private def replicateDelete(address:String, signature:ReplicationSignature, target:AbstractActor){
    try{

      val host = checkSignature(address.getBytes("UTF-8"), signature)

      if(host == null){
        log debug "Signature check failed"
        return
      }

      val storage = storageManager.storageForId(AddressGenerator.storageForAddress(address))

      if(storage.mode.allowWrite){

        storage.get(address) match {
          case Some(r) => storage.delete(address)
          case None =>
        }

        val calculator = new ReplicationSignatureCalculator(localSystemId, host)

        target ! ReplicateDeleteAckMsg(address, calculator.calculate(address))

        accessMediator ! ResourceDeletedMsg(address, 'ReplicationManager)
      }else{
        log warn "Failed to replicate delete, storage is not writable"
      }


    }catch{
      case e => log.error("Failed to replicate update", e)
    }
  }

  private def processConfiguration(configuration:String, signature:ReplicationSignature) = {

    if(checkSignature(configuration, signature) != null){
      log info "Processing configuration"
      configurationManager.processSerializedRemoteConfiguration(configuration)
    }else{
      log info "Ignorring configuration due to incorrect message"
    }
  }

  private def compareUpdatedResource(incomeResource:Resource, storedResource:Resource) = {

    for(i <- 0 to incomeResource.versions.length - 1){

      val incomeVersion = incomeResource.versions(i)

      if(storedResource.versions.length > i){
        val storedVersion = storedResource.versions(i)

        if(storedVersion.date != incomeVersion.date){
          incomeVersion.persisted = false
        }

      }else{
        incomeVersion.persisted = false
      }

    }
  }

  private def fillWithData(resource:Resource, replicationHost:ReplicationHost) = {

    val domainId = resource.systemMetadata.get(Domain.MD_FIELD).get

    val domain = domainManager.domainById(domainId) match{
      case Some(d) => d
      case None => throw new ReplicationException("Failed to replicate resource: " + resource.address + " due to unknown domain")
    }

    for(i <- 0 to resource.versions.size - 1){
      val version = resource.versions(i)
      val data = new RemoteSystemDataStream(replicationHost, useSecureDataConnection, resource.address, i+1, domain.id, domain.key)
      version.data = data
    }
  }

  private def checkSignature(bytes:Array[Byte], signature:ReplicationSignature):ReplicationHost = {

    ReplicationSignatureCalculator.foundAndVerify(bytes, signature, config) match{
      case Some(host) => host
      case None => null
    }
  }

  private def checkSignature(data:String, signature:ReplicationSignature):ReplicationHost = {

    ReplicationSignatureCalculator.foundAndVerify(data, signature, config) match{
      case Some(host) => host
      case None => null
    }
  }

}

case class ProcessAddMsg(
            val resource:Array[Byte],
            val signature:ReplicationSignature,
            val target:AbstractActor
        )

case class ProcessUpdateMsg(
            val resource:Array[Byte],
            val signature:ReplicationSignature,
            val target:AbstractActor
        )


case class ProcessDeleteMsg(
            val address:String,
            val signature:ReplicationSignature,
            val target:AbstractActor
        )