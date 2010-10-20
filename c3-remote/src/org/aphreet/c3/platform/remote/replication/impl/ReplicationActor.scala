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

import actors.Actor
import actors.remote.RemoteActor._
import org.springframework.stereotype.Component
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.remote.replication.{ReplicateUpdate, ReplicateDelete, ReplicateAdd}
import org.springframework.context.annotation.Scope
import org.aphreet.c3.platform.storage.StorageManager
import org.springframework.beans.factory.annotation.Autowired
import com.twmacinta.util.MD5
import org.aphreet.c3.platform.resource.{AddressGenerator, Resource}
import org.aphreet.c3.platform.remote.api.management.ReplicationHost
import org.apache.commons.logging.LogFactory

@Component
@Scope("singleton")
class ReplicationActor extends Actor{

  val log = LogFactory getLog getClass

  var storageManager:StorageManager = null

  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}

  var config:Map[String, ReplicationHost] = Map()

  def startWithConfig(config:Map[String, ReplicationHost]) = {

    log info "Starting replication actor..."

    this.config = config

    this.start

    alive(ReplicationConstants.REPLICATION_PORT)
    register('ReplicationActor, this)
  }

  def updateConfig(config:Map[String, ReplicationHost]) = {
    this.config = config
  }

  override def act{
    while(true){
      receive{
        case DestroyMsg => this.exit
        case ReplicateAdd(resource, signature) => replicateAdd(resource, signature)
        case ReplicateUpdate(resource, signature) => replicateUpdate(resource, signature)
        case ReplicateDelete(address, signature) => replicateDelete(address, signature)
      }
    }
  }

  private def replicateAdd(bytes:Array[Byte], signature:String){


    log debug "Replicating incoming add"
    try{
      val host = checkSignature(bytes, signature)

      if(host != null){
        val resource = Resource.fromByteArray(bytes)
        fillWithData(resource, host.hostname)

        val storage = storageManager.storageForResource(resource)

        if(storage.mode.allowWrite){
          resource.verifyCheckSums
          storage.put(resource)
        }
      }else{
        log debug "Signature check failed"
      }
    }catch{
      case e => log.error("Failed to replicate add", e)
    }
  }

  private def replicateUpdate(bytes:Array[Byte], signature:String){

    log debug "Replicating incoming update"

    try{
      val host = checkSignature(bytes, signature)
      if(host != null){
        val resource = Resource.fromByteArray(bytes)
        fillWithData(resource, host.hostname)

        val storage = storageManager.storageForResource(resource)

        if(storage.mode.allowWrite){

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

        }
      }else log debug "Signature check failed"
    }catch{
      case e => log.error("Failed to replicate update", e)
    }
  }

  private def replicateDelete(address:String, signature:String){
    try{
      if(checkSignature(address.getBytes("UTF-8"), signature) != null){
        val storage = storageManager.storageForId(AddressGenerator.storageForAddress(address))

        if(storage.mode.allowWrite){
          storage.delete(address)
        }
      }else log debug "Signature check failed"

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

  private def fillWithData(resource:Resource, host:String) = {
    for(i <- 0 to resource.versions.size - 1){
      val version = resource.versions(i)
      val data = new RemoteSystemDataWrapper(host, resource.address, i)
      version.data = data
    }
  }

  private def checkSignature(bytes:Array[Byte], signature:String):ReplicationHost = {

    val parts = signature.split(":", 2)

    if(parts.length != 2) return null

    val remoteSystemId = parts(0)
    val receivedHash = parts(1)

    val host = config.get(remoteSystemId) match {
      case Some(host) => host
      case None => null
    }

    if(host == null) return null

    val md5 = new MD5
    md5.Update(bytes)
    md5.Update(host.systemId)
    md5.Update(host.key)
    md5.Final

    val calculatedHash = md5.asHex

    if(receivedHash == calculatedHash){
      host
    }else{
      null
    }
  }

  @PreDestroy
  def destroy{
    this ! DestroyMsg
  }
}