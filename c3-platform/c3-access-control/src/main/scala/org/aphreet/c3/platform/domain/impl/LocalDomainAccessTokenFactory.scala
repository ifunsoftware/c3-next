package org.aphreet.c3.platform.domain.impl

import org.springframework.stereotype.Component
import org.aphreet.c3.platform.accesscontrol.{LocalAccess, RemoteAccess, AccessType}
import org.aphreet.c3.platform.domain.{DomainException, Domain}

@Component
class LocalDomainAccessTokenFactory extends DomainAccessTokenFactory {

  def retrieveDomain(accessParams: Map[String, String]):Domain = {

    accessParams.get("domain") match {
      case Some(domainId) => domainManager.domainById(domainId) match {
        case Some(domain) => domain
        case None => throw new DomainException("Unknown domain " + domainId)
      }
      case None => domainManager.getDefaultDomain
    }
  }

  def supportsAccess(accessType: AccessType):Boolean = {
    accessType match {
      case RemoteAccess => false
      case LocalAccess => true
    }
  }

}
