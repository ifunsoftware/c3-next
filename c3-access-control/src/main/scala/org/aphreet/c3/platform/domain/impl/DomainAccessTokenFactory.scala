/*
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

import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.domain._
import org.aphreet.c3.platform.accesscontrol._
import org.springframework.stereotype.Component

@Component
class DomainAccessTokenFactory extends AccessTokenFactory{

  @Autowired
  var domainManager:DomainManager = null

  @Autowired
  var accessControlManager:AccessControlManager = null

  @PostConstruct
  def init(){
    accessControlManager.registerFactory(this)
  }

  @PreDestroy
  def destroy(){
    accessControlManager.unregisterFactory(this)
  }

  def createAccessToken(action:Action, accessParams:Map[String, String]):AccessToken = {
    import DomainAccessTokenFactory._

    val domain = accessParams.get(DOMAIN_HEADER) match {
      case None => domainManager.getAnonymousDomain
      case Some(requestedDomain) => {

        val requestUri = accessParams.getOrElse(REQUEST_KEY, "")

        val date = accessParams.getOrElse(DATE_HEADER, "")

        if(date == ""){
          throw new DomainException("x-c3-date is empty")
        }

        val hashBase = requestUri + date + requestedDomain

        val hash = accessParams.getOrElse(SIGN_HEADER, "")

        if(hash == ""){
          throw new DomainException("x-c3-sign is empty")
        }

        domainManager.checkDomainAccess(requestedDomain, hash, hashBase)
      }
    }

    domain.mode match{
      case DisabledMode => throw new DomainException("Domain is disabled")
      case FullMode => new DomainAccessToken(action, domain)
      case ReadOnlyMode =>
        if(action == READ){
          new DomainAccessToken(action, domain)
        }else{
          throw new DomainException("Domain is readonly")
        }
    }
  }

}

object DomainAccessTokenFactory{

  val DOMAIN_HEADER = "x-c3-domain"
  val SIGN_HEADER = "x-c3-sign"
  val DATE_HEADER = "x-c3-date"
  val REQUEST_KEY = "x-c3-request-uri"

}