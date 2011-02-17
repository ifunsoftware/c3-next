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
import org.aphreet.c3.platform.resource.{AddressGenerator, Resource}
import org.aphreet.c3.platform.storage.StorageManager

import eu.medsea.mimeutil.MimeUtil

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


import org.aphreet.c3.platform.access._
import org.apache.commons.logging.LogFactory
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.common.msg._
import org.aphreet.c3.platform.management.{SPlatformPropertyListener, PropertyChangeEvent}

@Component("accessManager")
class AccessManagerImpl extends AccessManager with SPlatformPropertyListener{


  private val MIME_DETECTOR_CLASS = "c3.platform.mime.detector"

  var storageManager:StorageManager = _

  var accessMediator:AccessMediator = _

  val log = LogFactory.getLog(getClass)

  {
    log info "Starting AccessManager"
    start
  }

  @PreDestroy
  def destroy{
    log info "Stopping AccessManager"
    this ! DestroyMsg
  }

  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}

  @Autowired
  def setAccessMediator(mediator:AccessMediator) = {accessMediator = mediator}

  def get(ra:String):Resource = {

    if(log.isDebugEnabled){
      log.debug("Getting resource with address: " + ra)
    }

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

    if(log.isDebugEnabled){
      log.debug("Adding new resources")
    }

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


    val storage = storageManager.storageForResource(resource)

    if(storage != null){
      resource.calculateCheckSums
      val ra = storage.add(resource)

      accessMediator ! ResourceAddedMsg(resource)

      if(log.isDebugEnabled){
        log.debug("Resource added: " + ra)
      }

      ra
    }else{
      throw new StorageNotFoundException("Failed to find storage for resource")
    }
  }

  def update(resource:Resource):String = {

    if(log.isDebugEnabled){
      log.debug("Updating resource with address: " + resource.address)
    }

    try{
      val storage = storageManager.storageForId(AddressGenerator.storageForAddress(resource.address))
      if(storage.mode.allowWrite){
        resource.calculateCheckSums
        val ra = storage.update(resource)

        accessMediator ! ResourceUpdatedMsg(resource)

        ra

      }else{
        throw new StorageIsNotWritableException(storage.id)
      }
    }catch{
      case e:StorageNotFoundException => throw new ResourceNotFoundException(e)
    }
  }

  def delete(ra:String) = {

    if(log.isDebugEnabled){
      log.debug("Deleting resource with address: " + ra)
    }

    try{
      val storage = storageManager.storageForId(AddressGenerator.storageForAddress(ra))

      if(storage.mode.allowWrite){
        storage delete ra

        accessMediator ! ResourceDeletedMsg(ra)
      }

      else
        throw new StorageIsNotWritableException(storage.id)
    }catch{
      case e:StorageNotFoundException => throw new ResourceNotFoundException(e)
    }
  }

  def lock(ra:String) = {

    if(log.isDebugEnabled){
      log.debug("Locking address: " + ra)
    }

    try{
      val storage = storageManager.storageForId(AddressGenerator.storageForAddress(ra))

      if(storage.mode.allowWrite)
        storage.lock(ra)
      else
        throw new StorageIsNotWritableException(storage.id)

    }catch{
      case e:StorageNotFoundException => throw new ResourceNotFoundException(e)
    }
  }

  def unlock(ra:String) = {

    if(log.isDebugEnabled){
      log.debug("Unlocking address: " + ra)
    }

    try{
      val storage = storageManager.storageForId(AddressGenerator.storageForAddress(ra))

      if(storage.mode.allowWrite)
        storage.lock(ra)
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
        case DestroyMsg => {
          log info "Stopping AccessManagerActor"
          exit
        }
      }
    }
  }

  def propertyChanged(event:PropertyChangeEvent) = {

    log info "Received new value for mime detector: " + event.newValue

    if(event.oldValue != null){
      MimeUtil unregisterMimeDetector event.oldValue
    }

    MimeUtil registerMimeDetector event.newValue
  }

  def defaultValues:Map[String,String] =
    Map(MIME_DETECTOR_CLASS -> "eu.medsea.mimeutil.detector.ExtensionMimeDetector")

}