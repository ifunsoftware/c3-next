package org.aphreet.c3.platform.domain.impl

import org.aphreet.c3.platform.accesscontrol.{LocalAccess, RemoteAccess, AccessType}
import org.aphreet.c3.platform.auth.HashUtil
import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.domain.{DomainException, DomainManager, Domain}

class RestDomainAccessTokenFactory(val domainManager: DomainManager) extends DomainAccessTokenFactory {

  val log = Logger(getClass)

  def supportsAccess(accessType: AccessType): Boolean = {
    accessType match {
      case RemoteAccess => true
      case LocalAccess => false
    }
  }

  def retrieveDomain(accessParams: Map[String, String]): Domain = {
    import RestDomainAccessTokenFactory._

    val requestedDomain = accessParams.getOrElse(DOMAIN_HEADER, domainManager.getDefaultDomainId)

    val requestUri = accessParams.getOrElse(REQUEST_KEY, "")

    val date = accessParams.getOrElse(DATE_HEADER, "")

    val hashBase = requestUri + date + requestedDomain

    val hash = accessParams.getOrElse(SIGN_HEADER, "")

    verifySignatures(requestedDomain, hash, hashBase)
  }

  def verifySignatures(requestedDomain: String, hash: String, keyBase: String): Domain = {
    domainManager.findDomain(requestedDomain) match {
      case None => throw new DomainException("Requested domain " + requestedDomain + " not found")
      case Some(domain) => {
        val key = domain.key

        if (key.isEmpty) {
          domain
        } else {
          if (HashUtil.hmac(key, keyBase) == hash) {
            domain
          } else {
            log.warn("Incorrect access attempt for signature base '" + keyBase + "' and key '" + key + "'")
            throw new DomainException("Incorrect signature for signature base " + keyBase)
          }
        }
      }
    }
  }
}

object RestDomainAccessTokenFactory {

  val DOMAIN_HEADER = "x-c3-domain"
  val SIGN_HEADER = "x-c3-sign"
  val DATE_HEADER = "x-c3-date"
  val REQUEST_KEY = "x-c3-request-uri"

}
