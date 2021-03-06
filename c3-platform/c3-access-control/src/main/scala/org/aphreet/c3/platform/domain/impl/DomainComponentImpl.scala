/**
 * Copyright (c) 2011, Mikhail Malygin
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

package org.aphreet.c3.platform.domain.impl

import collection.immutable.HashMap
import java.lang.Integer
import java.util.{Random, UUID}
import org.aphreet.c3.platform.access.CleanupComponent
import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.config.{PlatformConfigComponent, ConfigAccessor}
import org.aphreet.c3.platform.domain._
import org.aphreet.c3.platform.exception.PlatformException
import scala.Some

trait DomainComponentImpl extends DomainComponent {

  this: CleanupComponent
    with PlatformConfigComponent =>

  val domainManager: DomainManager = new DomainManagerImpl(new DomainAccessor(configPersister))

  class DomainManagerImpl(val domainAccessor: ConfigAccessor[DomainConfig]) extends DomainManager {

    val log = Logger(classOf[DomainComponentImpl])

    private var domains: HashMap[String, Domain] = new HashMap()

    private var domainById: HashMap[String, Domain] = new HashMap()

    private var defaultDomainId: String = _

    {
      log.info("Starting DomainManager")

      reloadDomainConfig()
    }

    private def reloadDomainConfig() {

      var map = new HashMap[String, Domain]

      var idMap = new HashMap[String, Domain]

      val config = domainAccessor.load

      for (domain <- config.domains) {
        map += ((domain.name, domain))
        idMap += ((domain.id, domain))
      }

      domains = map
      domainById = idMap

      defaultDomainId = config.defaultDomain
    }

    private def storeDomainConfig() {
      domainAccessor.store(DomainConfig(domains.values.toList, defaultDomainId))
    }

    def addDomain(name: String) {

      domains.get(name) match {
        case Some(x) => throw new PlatformException("Domain with such name already exists")
        case None =>
      }

      val domain = Domain(UUID.randomUUID.toString, name, generateKey, FullMode, deleted = false)

      domainAccessor.update(config => DomainConfig(domain :: config.domains, config.defaultDomain))

      reloadDomainConfig()
    }

    def generateKey(name: String): String = {
      domains.get(name) match {
        case Some(d) =>
          d.key = generateKey
          storeDomainConfig()
          reloadDomainConfig()
          d.key
        case None => throw new PlatformException("Domain with such name does not exists")
      }
    }


    def removeKey(name: String) {
      domains.get(name) match {
        case Some(d) =>
          d.key = ""
          storeDomainConfig()
          reloadDomainConfig()
        case None => throw new PlatformException("Domain with such name does not exists")
      }
    }

    def setMode(name: String, mode: String) {
      domains.get(name) match {
        case Some(d) =>
          d.mode = DomainMode.byName(mode)
          storeDomainConfig()
          reloadDomainConfig()
        case None => throw new PlatformException("Domain with such name does not exists")
      }
    }

    def updateName(name: String, newName: String) {
      domains.get(name) match {
        case Some(d) =>
          d.name = newName
          storeDomainConfig()
          reloadDomainConfig()
        case None => throw new PlatformException("Domain with such name does not exists")
      }
    }


    def deleteDomain(name: String) {
      domains.get(name) match {
        case Some(domain) => {
          if (domain.deleted) {
            throw new PlatformException("Domain with such name has been already deleted")
          } else if (defaultDomainId == domain.id) {
            throw new PlatformException("Default domain can't be deleted")
          } else {
            log.info("Deleting domain " + domain.id + " " + domain.name)
            domain.deleted = true
            storeDomainConfig()
            reloadDomainConfig()

            cleanupManager.cleanupResources(resource => resource.systemMetadata(Domain.MD_FIELD) match {
              case Some(value) => value == domain.id
              case None => false
            })
          }
        }
        case None => throw new PlatformException("Domain with such name does not exists")
      }
    }

    def domainList: List[Domain] = {
      domains.values.toList
    }

    def getDefaultDomain: Domain = {
      domainById.get(defaultDomainId) match {
        case Some(d) => d
        case None => throw new DomainException("Can't find default domain with id " + defaultDomainId)
      }
    }

    def setDefaultDomain(domainId: String) {
      if (domainById.contains(domainId)) {
        defaultDomainId = domainId
        storeDomainConfig()
      } else {
        throw new DomainException("Can't find domain with id " + domainId)
      }
    }

    def getDefaultDomainId: String = defaultDomainId

    def importDomain(importedDomain: Domain, remoteSystemId: String) {

      val domainConfig = domainAccessor.load

      val domainList = domainConfig.domains

      val newDomainList = DomainManagerImpl.addDomainToList(importedDomain, remoteSystemId, domainList)

      domainAccessor.store(DomainConfig(newDomainList, domainConfig.defaultDomain))

      reloadDomainConfig()
    }


    def domainById(id: String): Option[Domain] = {
      domainById.get(id)
    }

    def findDomain(idOrName: String): Option[Domain] = {
      (domainById.get(idOrName) match {
        case Some(domain) => Some(domain)
        case None => domains.get(idOrName)
      }).filter(!_.deleted)
    }

    def generateKey: String = {
      val random = new Random()

      String.format("%08x%08x%08x%08x", new Integer(random.nextInt),
        new Integer(random.nextInt),
        new Integer(random.nextInt),
        new Integer(random.nextInt))
    }

  }

}

object DomainManagerImpl {

  val log = Logger(getClass)

  def addDomainToList(importedDomain: Domain, remoteSystemId: String, domainList: List[Domain]): List[Domain] = {
    domainList.find(d => d.id == importedDomain.id) match {
      case Some(domain) =>

        if (domain.name != importedDomain.name
          || domain.key != importedDomain.key
          || domain.mode != importedDomain.mode
          || domain.deleted != importedDomain.deleted) {

          log.debug("Updating domain " + domain + " with imported domain: " + importedDomain)

          //Found domain with the same id
          //Overriding name of the domain
          domain.name = importedDomain.name
          domain.key = importedDomain.key
          domain.mode = importedDomain.mode
          domain.deleted = importedDomain.deleted
        }

        domainList

      case None => {
        domainList.find(d => d.name == importedDomain.name) match {
          case Some(domain) =>
            //We have a name collision for this domain, adding a remoteSystemId to its name
            importedDomain.name = importedDomain.name + "-" + remoteSystemId

            log info "Adding domain with name " + importedDomain.name

            importedDomain :: domainList
          case None =>
            //No collisions

            log info "Adding domain with name " + importedDomain.name

            importedDomain :: domainList
        }
      }

    }
  }

}