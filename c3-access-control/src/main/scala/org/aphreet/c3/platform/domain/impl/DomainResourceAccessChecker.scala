package org.aphreet.c3.platform.domain.impl

import org.aphreet.c3.platform.resource.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.domain._
import org.aphreet.c3.platform.accesscontrol.{AccessControlManager, READ, Action, ResourceAccessChecker}
import javax.annotation.{PreDestroy, PostConstruct}

/**
 * Copyright iFunSoftware 2011
 * @author Mikhail Malygin
 */

@Component
class DomainResourceAccessChecker extends ResourceAccessChecker{

  @Autowired
  var domainManager:DomainManager = null

  @Autowired
  var accessControlManager:AccessControlManager = null

  @PostConstruct
  def init(){
    accessControlManager.registerChecker(this)
  }

  @PreDestroy
  def destroy(){
    accessControlManager.unregisterChecker(this)
  }

  def canPerformActionWithResource(action:Action, resource:Resource, accessParams:Map[String, String]):Boolean = {

    import DomainResourceAccessChecker._
    
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

    val domainId = domain.mode match{
      case DisabledMode => throw new DomainException("Domain is disabled")
      case FullMode => domain.id
      case ReadOnlyMode =>
        if(action == READ){
          domain.id
        }else{
          throw new DomainException("Domain is readonly")
        }
    }

    resource.systemMetadata.get(Domain.MD_FIELD) match{
      case Some(id) => if(id != domainId) throw new DomainException("Requested resource does not belong to specified domain")
      case None =>
    }

    true
  }


}

object DomainResourceAccessChecker{

  val DOMAIN_HEADER = "x-c3-domain"
  val SIGN_HEADER = "x-c3-sign"
  val DATE_HEADER = "x-c3-date"
  val REQUEST_KEY = "x-c3-request-uri"

}