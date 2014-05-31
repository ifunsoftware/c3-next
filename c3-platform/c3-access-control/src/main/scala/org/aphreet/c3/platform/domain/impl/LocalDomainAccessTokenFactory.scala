package org.aphreet.c3.platform.domain.impl

import org.aphreet.c3.platform.accesscontrol.{LocalAccess, RemoteAccess, AccessType}
import org.aphreet.c3.platform.domain.{DomainManager, DomainException, Domain}

class LocalDomainAccessTokenFactory(val domainManager: DomainManager) extends DomainAccessTokenFactory {

  def retrieveDomain(accessParams: Map[String, String]): Domain = {

    accessParams.get("domain") match {
      case Some(domainId) => {
        domainManager.findDomain(domainId).getOrElse {
          throw new DomainException("Requested domain " + domainId + " not found")
        }
      }
      case None => domainManager.getDefaultDomain
    }
  }

  def supportsAccess(accessType: AccessType): Boolean = {
    accessType match {
      case RemoteAccess => false
      case LocalAccess => true
    }
  }

}
