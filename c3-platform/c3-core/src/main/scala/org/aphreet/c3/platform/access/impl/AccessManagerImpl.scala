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
import org.aphreet.c3.platform.resource.{ResourceAddress, Resource}
import org.aphreet.c3.platform.storage.StorageManager

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.access.Constants.ACCESS_MANAGER_NAME
import org.apache.commons.logging.LogFactory
import javax.annotation.PreDestroy
import org.aphreet.c3.platform.common.msg._
import org.aphreet.c3.platform.config.{PlatformConfigManager, SPlatformPropertyListener, PropertyChangeEvent}
import eu.medsea.util.EncodingGuesser
import eu.medsea.mimeutil.{TextMimeDetector, MimeUtil}
import org.aphreet.c3.platform.common.Constants
import collection.mutable

@Component("accessManager")
class AccessManagerImpl extends AccessManager with SPlatformPropertyListener{

  private val MIME_DETECTOR_CLASS = "c3.platform.mime.detector"

  @Autowired
  var storageManager:StorageManager = _

  @Autowired
  var accessMediator:AccessMediator = _

  @Autowired
  var configManager:PlatformConfigManager = _

  @Autowired
  var accessCache:AccessCache = _

  val log = LogFactory.getLog(getClass)

  val resourceOwners = new mutable.HashSet[ResourceOwner]

  lazy val systemId = getSystemId

  {
    log info "Starting AccessManager"

    configureDefaultMimeDetector()

    start()
  }

  @PreDestroy
  def destroy(){
    log info "Stopping AccessManager"
    this ! DestroyMsg
  }

  def get(ra:String):Resource = getInternal(ra)

  def getOption(ra:String):Option[Resource] = {
    try{
      Some(getInternal(ra))
    }catch {
      case e:Throwable => None
    }
  }

  protected def getInternal(ra:String):Resource = {

    if(log.isDebugEnabled){
      log.debug("Getting resource with address: " + ra)
    }

    accessCache.get(ra) match{
      case Some(resource) => return resource
      case None =>
    }

    try{

      val storage = storageManager.storageForAddress(ResourceAddress(ra))

      if(!storage.mode.allowRead){
        throw new StorageException("Storage is not readable")
      }

      storage.get(ra) match {
        case Some(r) => {
          accessCache.put(r)
          r
        }
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
    resource.address = ResourceAddress.generate(resource, systemId).stringValue


    val storage = storageManager.storageForResource(resource)

    if(storage != null){
      resource.calculateCheckSums
      val ra = storage.add(resource)

      accessMediator ! ResourceAddedMsg(resource, ACCESS_MANAGER_NAME)

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
      val storage = storageManager.storageForAddress(ResourceAddress(resource.address))

      if(storage.mode.allowWrite){
        resource.calculateCheckSums

        for(owner <- resourceOwners){
          if(!owner.resourceCanBeUpdated(resource)){
            log info "" + owner + " forbided resource update"
            throw new AccessException("Specified resource can't be updated")
          }
        }

        resource.systemMetadata.put(Resource.MD_UPDATED, System.currentTimeMillis().toString)

        val ra = storage.update(resource)

        for(owner <- resourceOwners){
          owner.updateResource(resource)
        }

        accessCache.remove(resource.address)

        accessMediator ! ResourceUpdatedMsg(resource, ACCESS_MANAGER_NAME)

        ra

      }else{
        throw new StorageIsNotWritableException(storage.id)
      }
    }catch{
      case e:StorageNotFoundException => throw new ResourceNotFoundException(e)
    }
  }

  def delete(ra:String) {

    if(log.isDebugEnabled){
      log.debug("Deleting resource with address: " + ra)
    }

    try{
      val storage = storageManager.storageForAddress(ResourceAddress(ra))

      if(storage.mode.allowWrite){

        val resource = storage.get(ra) match {
          case Some(r) => r
          case None => throw new ResourceNotFoundException()
        }

        for(owner <- resourceOwners){
          if(!owner.resourceCanBeDeleted(resource)){
            log info "" + owner + " forbided resource deletion"
            throw new AccessException("Specified resource can't be deleted")
          }
        }

        for(owner <- resourceOwners){
          owner.deleteResource(resource)
        }

        storage delete ra

        accessCache.remove(ra)

        accessMediator ! ResourceDeletedMsg(ra, ACCESS_MANAGER_NAME)
      }

      else
        throw new StorageIsNotWritableException(storage.id)
    }catch{
      case e:StorageNotFoundException => throw new ResourceNotFoundException(e)
    }
  }

  def registerOwner(owner:ResourceOwner) {
    resourceOwners.synchronized{
      log debug "Registering owner " + owner
      resourceOwners += owner
    }
  }

  def unregisterOwner(owner:ResourceOwner) {
    resourceOwners.synchronized{
      log debug "Unregistering owner " + owner
      resourceOwners -= owner
    }
  }

  def act(){
    loop{
      react{
        case UpdateMetadataMsg(address, metadata) =>{
          try{
            val storage = storageManager.storageForAddress(ResourceAddress(address))
            if(storage != null){
              storage.appendSystemMetadata(address, metadata)
            }
          }catch{
            case e: Throwable => log.warn("Failed to append metadata to resource: " + address + " msg is " + e.getMessage)
          }
        }
        case DestroyMsg => {
          log info "Stopping AccessManagerActor"
          exit()
        }
      }
    }
  }

  def propertyChanged(event:PropertyChangeEvent) {

    log info "Received new value for mime detector: " + event.newValue

    if(event.oldValue != null){
      MimeUtil unregisterMimeDetector event.oldValue
    }

    MimeUtil registerMimeDetector event.newValue
  }

  def defaultValues:Map[String,String] =
    Map(MIME_DETECTOR_CLASS -> "eu.medsea.mimeutil.detector.MagicMimeMimeDetector")

  def configureDefaultMimeDetector() {

    MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector")

    val encodings = new java.util.HashSet[String]
    encodings.add("UTF-8")
    encodings.add("cp1251")
    encodings.add("US-ASCII")
    encodings.add("UTF-16")
    encodings.add("KOI8-R")

    EncodingGuesser.setSupportedEncodings(encodings)
    TextMimeDetector.setPreferredEncodings(Array("UTF-8"))
  }

  private def getSystemId:String = {
      configManager.getPlatformProperties.get(Constants.C3_SYSTEM_ID) match {
        case Some(value) => value
        case None => throw new ConfigurationException("Failed to get systemId from params")
      }
    }
}