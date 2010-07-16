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

package org.aphreet.c3.platform.access.impl

import org.aphreet.c3.platform.exception._
import org.aphreet.c3.platform.management.PropertyChangeEvent
import org.aphreet.c3.platform.resource.{AddressGenerator, Resource}
import org.aphreet.c3.platform.storage.StorageManager

import eu.medsea.mimeutil.MimeUtil

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


import collection.immutable.{Set, HashSet}
import org.aphreet.c3.platform.access._
import actors.Actor
import actors.Actor._
import org.apache.commons.logging.LogFactory
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.common.msg._

@Component("accessManager")
class AccessManagerImpl extends AccessManager{


  private val MIME_DETECTOR_CLASS = "c3.platform.mime.detector"

  var storageManager:StorageManager = _

  var accessListeners:Set[Actor] = new HashSet

  val log = LogFactory.getLog(getClass)

  @PostConstruct
  def init{
    log info "Starting AccessManager"
    this.start
  }

  @PreDestroy
  def destroy{
    log info "Stopping AccessManager"
    this ! DestroyMsg
  }

  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}

  def get(ra:String):Resource = {
    try{
      val storage = storageManager.storageForId(AddressGenerator.storageForAddress(ra))

      if(!storage.mode.allowRead){
        throw new StorageException("Storage is not readable")
      }

      storage.get(ra) match {
        case Some(r) => r
        case None => throw new ResourceNotFoundException(ra)
      }
    }catch{
      case e:StorageNotFoundException => throw new ResourceNotFoundException(e)
    }

  }

  def add(resource:Resource):String = {

    val contentType = resource.metadata.get(Resource.MD_CONTENT_TYPE) match {
      case None => resource.versions(0).data.mimeType
      case Some(x) => if(!x.isEmpty) x else resource.versions(0).data.mimeType
    }

    resource.systemMetadata.put(Resource.MD_CONTENT_TYPE, contentType)
    resource.metadata.put(Resource.MD_CONTENT_TYPE, contentType)

    val pool = resource.metadata.get(Resource.MD_POOL) match {
      case Some(x) => if(!x.isEmpty) x else "default"
      case None => "default"
    }

    resource.systemMetadata.put(Resource.MD_POOL, pool)
    resource.metadata.put(Resource.MD_POOL, pool)


    val storage = storageManager.storageForResource(resource)

    if(storage != null){
      resource.calculateCheckSums
      val ra = storage.add(resource)

      accessListeners.foreach{
        _ ! ResourceAddedMsg(resource)
      }

      ra
    }else{
      throw new StorageNotFoundException("Failed to find storage for resource")
    }
  }

  def update(resource:Resource):String = {
    try{
      val storage = storageManager.storageForId(AddressGenerator.storageForAddress(resource.address))
      if(storage.mode.allowWrite){
        resource.calculateCheckSums
        val ra = storage.update(resource)

        accessListeners.foreach{
          _ ! ResourceUpdatedMsg(resource)
        }

        ra

      }else{
        throw new StorageIsNotWritableException(storage.id)
      }
    }catch{
      case e:StorageNotFoundException => throw new ResourceNotFoundException(e)
    }
  }

  def delete(ra:String) = {
    try{
      val storage = storageManager.storageForId(AddressGenerator.storageForAddress(ra))

      if(storage.mode.allowWrite){
        storage delete ra

        accessListeners.foreach {
          _ ! ResourceDeletedMsg(ra)
        }
      }

      else
        throw new StorageIsNotWritableException(storage.id)
    }catch{
      case e:StorageNotFoundException => throw new ResourceNotFoundException(e)
    }
  }

  def act{
    loop{
      react{
        case UpdateMetadataMsg(address, metadata) =>{
          try{
          val storage = storageManager.storageForId(AddressGenerator.storageForAddress(address))
          if(storage != null){
            storage.appendSystemMetadata(address, metadata)
          }
          }catch{
            case e=> log.warn("Failed to append metadata to resource: " + address + " msg is " + e.getMessage)
          }
        }

        case RegisterListenerMsg(actor) =>
          log debug "Registering listener " + actor.toString
          accessListeners = accessListeners + actor
          log debug accessListeners.toString
        case UnregisterListenerMsg(actor) =>
          log debug "Unregistering listener " + actor.toString
          accessListeners = accessListeners - actor
          log debug accessListeners.toString

        case DestroyMsg => this.exit
      }
    }
  }

  def listeningForProperties:Array[String] = Array(MIME_DETECTOR_CLASS)

  def propertyChanged(event:PropertyChangeEvent) = {

    if(event.oldValue != null){
      MimeUtil unregisterMimeDetector event.oldValue
    }

    MimeUtil registerMimeDetector event.newValue
  }

  def defaultValues:Map[String,String] =
    Map(MIME_DETECTOR_CLASS -> "eu.medsea.mimeutil.detector.ExtensionMimeDetector")
  
}