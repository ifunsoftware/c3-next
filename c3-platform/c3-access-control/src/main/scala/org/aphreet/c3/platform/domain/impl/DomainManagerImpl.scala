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

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct
import org.aphreet.c3.platform.exception.PlatformException
import collection.immutable.{HashMap}
import org.springframework.context.annotation.Scope
import org.aphreet.c3.platform.domain._
import org.aphreet.c3.platform.auth.HashUtil
import java.util.{Random, UUID}
import java.lang.Integer
import org.apache.commons.logging.LogFactory
import scala.Some
import org.aphreet.c3.platform.config.PlatformConfigManager

@Component("domainManager")
@Scope("singleton")
class DomainManagerImpl extends DomainManager{

  val log = LogFactory getLog getClass

  @Autowired
  var domainAccessor:DomainAccessor = _

  @Autowired
  var platformConfigManager: PlatformConfigManager = _

  private var domains:HashMap[String, Domain] = new HashMap()

  private var domainById:HashMap[String, Domain] = new HashMap()


  @PostConstruct
  def init(){
    reloadDomainConfig()
  }

  private def reloadDomainConfig() {

    var map = new HashMap[String, Domain]

    var idMap = new HashMap[String, Domain]

    for(domain <- domainAccessor.load){
      map += ((domain.name, domain))
      idMap += ((domain.id, domain))
    }

    domains = map
    domainById = idMap
  }

  private def storeDomainConfig() {
    domainAccessor.store(domains.values.toList)
  }

  def addDomain(name:String) {

    domains.get(name) match {
      case Some(x) => throw new PlatformException("Domain with such name already exists")
      case None =>
    }

    val domain = Domain(UUID.randomUUID.toString, name, generateKey, FullMode)

    domainAccessor.update(l => domain :: l)

    reloadDomainConfig()
  }

  def generateKey(name:String):String = {
    domains.get(name) match {
      case Some(d) =>
        d.key = generateKey
        storeDomainConfig()
        reloadDomainConfig()
        d.key
      case None => throw new PlatformException("Domain with such name does not exists")
    }
  }

  def setMode(name:String, mode:String) {
    domains.get(name) match {
      case Some(d) =>
        d.mode = DomainMode.byName(mode)
        storeDomainConfig()
        reloadDomainConfig()
      case None => throw new PlatformException("Domain with such name does not exists")
    }
  }

  def updateName(name:String, newName:String) {
    domains.get(name) match {
      case Some(d) =>
        d.name = newName
        storeDomainConfig()
        reloadDomainConfig()
      case None => throw new PlatformException("Domain with such name does not exists")
    }
  }

  def domainList:List[Domain] = {
    domains.values.toList
  }

  def getAnonymousDomain:Domain = {
    domains.get("anonymous") match {
      case Some(d) => d
      case None => throw new DomainException("Can't find anonymous domain")
    }
  }

  def importDomain(importedDomain:Domain, remoteSystemId:String){

    val domainList = domainAccessor.load

    val newDomainList = addDomainToList(importedDomain, remoteSystemId, domainList)

    domainAccessor.store(newDomainList)

    reloadDomainConfig()
  }

  def addDomainToList(importedDomain:Domain, remoteSystemId:String, domainList:List[Domain]):List[Domain] = {
    domainList.filter(d => d.id == importedDomain.id).headOption match{
      case Some(domain) =>
        //Found domain with the same id
          //Overriding name of the domain
        domain.name = importedDomain.name
        domain.key = importedDomain.key
        domain.mode = importedDomain.mode

        domainList

      case None => {
        domainList.filter(d => d.name == importedDomain.name).headOption match{
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

  def domainById(id:String):Option[Domain] = {
    domainById.get(id)
  }


  def checkDomainAccess(name:String, hash:String, keyBase:String):Domain = {

    domains.get(name) match{
      case Some(d) => {
        val key = d.key

        if(key.isEmpty){
          d
        }else{
          if(HashUtil.hmac(key, keyBase) == hash){
            d
          }else{
            log.warn("Incorect access attempt for keybase '" + keyBase + "' and key '" + key + "'")
            throw new DomainException("Incorrect signature")
          }
        }
      }
      case None => {
        domainById.get(name) match {
          case Some(d) => {
            val key = d.key
            if(!key.isEmpty){
              if(HashUtil.hmac(key, keyBase) == hash){
                d
              }else{
                log.warn("Incorect access attempt for keybase '" + keyBase + "' and key '" + key + "'")
                throw new DomainException("Incorrect signature")
              }
            }else{
              d
            }
          }
          case None => {
            throw new DomainException("Domain not found")
          }
        }
      }
    }
  }

  def generateKey:String = {
    val random = new Random()

    String.format("%08x%08x%08x%08x", new Integer(random.nextInt),
      new Integer(random.nextInt),
      new Integer(random.nextInt),
      new Integer(random.nextInt))
  }

}