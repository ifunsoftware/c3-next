package org.aphreet.c3.platform.domain.impl

import org.aphreet.c3.platform.domain.{DomainException, Domain}
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.accesscontrol.{LocalAccess, RemoteAccess, AccessType}

@Component
class RestDomainAccessTokenFactory extends DomainAccessTokenFactory{

  def supportsAccess(accessType: AccessType):Boolean = {
    accessType match {
      case RemoteAccess => true
      case LocalAccess => false
    }
  }

  def retrieveDomain(accessParams: Map[String, String]):Domain = {
    import RestDomainAccessTokenFactory._

    accessParams.get(DOMAIN_HEADER) match {
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
  }
}

object RestDomainAccessTokenFactory{

  val DOMAIN_HEADER = "x-c3-domain"
  val SIGN_HEADER = "x-c3-sign"
  val DATE_HEADER = "x-c3-date"
  val REQUEST_KEY = "x-c3-request-uri"

}
