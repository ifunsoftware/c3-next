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

import collection.mutable
import eu.medsea.mimeutil.{TextMimeDetector, MimeUtil}
import eu.medsea.util.EncodingGuesser
import org.aphreet.c3.platform.access.Constants.ACCESS_MANAGER_NAME
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.common.{ComponentLifecycle, Logger, Constants}
import org.aphreet.c3.platform.config._
import org.aphreet.c3.platform.exception._
import org.aphreet.c3.platform.resource.{ResourceAddress, Resource}
import org.aphreet.c3.platform.statistics.StatisticsComponent
import org.aphreet.c3.platform.storage.StorageComponent
import org.aphreet.c3.platform.storage.dispatcher.selector.mime.MimeTypeStorageSelectorComponent
import scala.Some
import org.aphreet.c3.platform.storage.updater.StorageUpdaterComponent
import org.aphreet.c3.platform.actor.ActorComponent
import akka.actor.{Actor, Props}

trait AccessComponentImpl extends AccessComponent with CleanupComponent with ComponentLifecycle{

  this: PlatformConfigComponent
    with StatisticsComponent
    with StorageComponent
    with StorageUpdaterComponent
    with MimeTypeStorageSelectorComponent
    with ActorComponent =>

  val accessMediator = new AccessMediatorImpl(actorSystem)

  private val accessCache = new AccessCacheImpl(actorSystem, accessMediator.async, statisticsManager.async)

  val accessManager = new AccessManagerImpl

  val cleanupManager = new CleanupManagerImpl(storageUpdater, accessMediator.async)

  val accessCounter = actorSystem.actorOf(Props.create(classOf[AccessCounter], accessMediator.async, statisticsManager.async))

  class AccessManagerImpl extends AccessManager with SPlatformPropertyListener {

    private val MIME_DETECTOR_CLASS = "c3.platform.mime.detector"
    private val USE_ACCESS_CACHE = "c3.platform.cache.enabled"

    val log = Logger(classOf[AccessComponentImpl])

    val resourceOwners = new mutable.HashSet[ResourceOwner]

    private var useAccessCache = false

    lazy val systemId = getSystemId

    val async = actorSystem.actorOf(Props.create(classOf[AccessManagerActor], this))

    {
      log info "Starting AccessManager"

      configureDefaultMimeDetector()

      platformConfigManager.async ! RegisterMsg(this)
    }

    def get(ra: String): Resource = {
      getInternal(ra) match {
        case Some(resource) => resource
        case None => throw new ResourceNotFoundException()
      }
    }

    def getOption(ra: String): Option[Resource] = getInternal(ra)

    protected def getInternal(ra: String): Option[Resource] = {

      log.debug("Getting resource with address: {}", ra)

      readCached(ra) match {
        case None =>
          cache(
            storageManager.storageForAddress(ResourceAddress(ra)).filter(_.mode.allowRead)
              .flatMap(storage => storage.get(ra))
          )
        case Some(resource) => Some(resource)
      }
    }

    def add(resource: Resource): String = {

      log.debug("Adding new resource")

      val contentType = resource.metadata(Resource.MD_CONTENT_TYPE) match {
        case None => resource.versions(0).data.mimeType
        case Some(x) => if (!x.isEmpty) x else resource.versions(0).data.mimeType
      }

      resource.metadata(Resource.MD_CONTENT_TYPE) = contentType
      resource.address = ResourceAddress.generate(resource, systemId).stringValue

      resource.isVersioned = mimeStorageSelector.storageTypeForResource(resource)

      storageManager.storageForResource(resource) match {
        case Some(storage) => {
          val ra = storage.add(resource.calculateCheckSums)

          accessMediator.async ! ResourceAddedMsg(resource, ACCESS_MANAGER_NAME)

          log.debug("Resource added: {}", ra)

          ra
        }
        case None => throw new StorageNotFoundException("Failed to find storage for resource")
      }
    }

    def update(resource: Resource): String = {

      log.debug("Updating resource with address: {}", resource.address)

      storageManager.storageForAddress(ResourceAddress(resource.address)) match {
        case Some(storage) => {
          if (storage.mode.allowWrite) {
            resource.calculateCheckSums

            for (owner <- resourceOwners) {
              if (!owner.resourceCanBeUpdated(resource)) {
                log info "" + owner + " forbided resource update"
                throw new AccessException("Specified resource can't be updated")
              }
            }

            resource.systemMetadata(Resource.MD_UPDATED) = System.currentTimeMillis().toString

            val ra = storage.update(resource)

            for (owner <- resourceOwners) {
              owner.updateResource(resource)
            }

            accessCache.remove(resource.address)

            accessMediator.async ! ResourceUpdatedMsg(resource, ACCESS_MANAGER_NAME)

            ra

          } else {
            throw new StorageIsNotWritableException(storage.id)
          }
        }
        case None => throw new ResourceNotFoundException()
      }
    }

    def delete(ra: String) {

      if (log.isDebugEnabled) {
        log.debug("Deleting resource with address: " + ra)
      }

      storageManager.storageForAddress(ResourceAddress(ra)).filter(_.mode.allowWrite) match {
        case Some(storage) => {

          val resource = storage.get(ra) match {
            case Some(r) => r
            case None => throw new ResourceNotFoundException()
          }

          for (owner <- resourceOwners) {
            if (!owner.resourceCanBeDeleted(resource)) {
              log info "" + owner + " forbade resource deletion"
              throw new AccessException("Specified resource can't be deleted")
            }
          }

          for (owner <- resourceOwners) {
            owner.deleteResource(resource)
          }


          storage delete ra

          accessCache.remove(ra)

          accessMediator.async ! ResourceDeletedMsg(ra, ACCESS_MANAGER_NAME)
        }

        case None => throw new ResourceNotFoundException()
      }
    }

    def registerOwner(owner: ResourceOwner) {
      resourceOwners.synchronized {
        log debug "Registering owner " + owner
        resourceOwners += owner
      }
    }

    def unregisterOwner(owner: ResourceOwner) {
      resourceOwners.synchronized {
        log debug "Unregistering owner " + owner
        resourceOwners -= owner
      }
    }

    class AccessManagerActor extends Actor{
      def receive = {
        case UpdateMetadataMsg(address, metadata, isSystem) => {
          try {
            storageManager.storageForAddress(ResourceAddress(address)).map(_.appendMetadata(address, metadata, isSystem))
            accessCache.remove(address)
          } catch {
            case e: Throwable => log.warn("Failed to append metadata to resource: " + address + " msg is " + e.getMessage)
          }
        }
      }
    }

    def propertyChanged(event: PropertyChangeEvent) {

      event.name match {
        case MIME_DETECTOR_CLASS => {
          log info "Received new value for mime detector: " + event.newValue

          if (event.oldValue != null) {
            MimeUtil unregisterMimeDetector event.oldValue
          }

          MimeUtil registerMimeDetector event.newValue
        }
        case USE_ACCESS_CACHE => {
          useAccessCache = event.newValue.toBoolean
          if (useAccessCache) {
            log.info("Access cache in enabled")
          } else {
            log.info("Access cache is disabled")
          }
        }
      }
    }

    def defaultValues: Map[String, String] =
      Map(
        MIME_DETECTOR_CLASS -> "eu.medsea.mimeutil.detector.MagicMimeMimeDetector",
        USE_ACCESS_CACHE -> "true"
      )

    def configureDefaultMimeDetector() {

      MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector")

      val encodings = new java.util.HashSet[String]
      encodings.add("UTF-8")
      encodings.add("cp1251")
      encodings.add("US-ASCII")
      encodings.add("UTF-16")

      EncodingGuesser.setSupportedEncodings(encodings)
      TextMimeDetector.setPreferredEncodings(Array("UTF-8"))
    }

    private def readCached(address: String): Option[Resource] = {
      if (useAccessCache) {
        accessCache.get(address)
      } else {
        None
      }
    }

    private def cache(resourceOption: Option[Resource]): Option[Resource] = {
      if (useAccessCache) {
        resourceOption.map(accessCache.put)
      }
      resourceOption
    }

    private def getSystemId: String = {
      platformConfigManager.getPlatformProperties.get(Constants.C3_SYSTEM_ID) match {
        case Some(value) => value
        case None => throw new ConfigurationException("Failed to get systemId from params")
      }
    }
  }
}