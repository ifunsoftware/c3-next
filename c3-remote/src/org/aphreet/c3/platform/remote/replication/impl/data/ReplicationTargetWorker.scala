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

import org.aphreet.c3.platform.remote.api.management.ReplicationHost
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.resource.{AddressGenerator, Resource}
import org.aphreet.c3.platform.remote.replication.{ReplicateAddAckMsg, ReplicateDeleteAckMsg, ReplicateUpdateAckMsg, ReplicationSignature}
import org.aphreet.c3.platform.storage.StorageManager
import actors.{AbstractActor, Actor}

class ReplicationTargetWorker(val storageManager:StorageManager) extends Actor {

  val log = LogFactory getLog getClass

  var config:Map[String, ReplicationHost] = Map()

  def startWithConfig(config:Map[String, ReplicationHost]) = {

    log info "Starting replication worker"

    this.config = config

    this.start
  }

  def updateConfig(config:Map[String, ReplicationHost]) = {
    this.config = config
  }

  override def act{
    loop{
      react{

        case ProcessAddMsg(bytes, sign, target) => replicateAdd(bytes, sign, target)

        case ProcessUpdateMsg(bytes, sign, target) => replicateUpdate(bytes, sign, target)

        case ProcessDeleteMsg(address, sign, target) => replicateDelete(address, sign, target)

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

      val resource = Resource.fromByteArray(bytes)

      val storage = storageManager.storageForResource(resource)

      if(!storage.mode.allowWrite){
        log debug "Failed to replicate resource, storage is not writtable"
        return
      }

      fillWithData(resource, host)
      resource.verifyCheckSums
      storage.put(resource)

      val calculator = new ReplicationSignatureCalculator(host)

      target ! ReplicateAddAckMsg(resource.address, calculator.calculate(resource.address))

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

      val resource:Resource = Resource.fromByteArray(bytes)

      val storage = storageManager.storageForResource(resource)

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
        }
        case None => {
          resource.verifyCheckSums
          storage.put(resource)
        }
      }

      val calculator = new ReplicationSignatureCalculator(host)

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

        val calculator = new ReplicationSignatureCalculator(host)

        target ! ReplicateDeleteAckMsg(address, calculator.calculate(address))
      }else{
        log warn "Failed to replicate delete, storage is not writable"
      }


    }catch{
      case e => log.error("Failed to replicate update", e)
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
    for(i <- 0 to resource.versions.size - 1){
      val version = resource.versions(i)
      val data = new RemoteSystemDataWrapper(replicationHost, resource.address, i)
      version.data = data
    }
  }

  private def checkSignature(bytes:Array[Byte], signature:ReplicationSignature):ReplicationHost = {

    ReplicationSignatureCalculator.foundAndVerify(bytes, signature, config) match{
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