package org.aphreet.c3.platform.remote.replication.impl.config

import org.aphreet.c3.platform.domain.{DomainMode, Domain}

/*
* Created by IntelliJ IDEA.
* User: Aphreet
* Date: 6/12/11
* Time: 7:32 PM
*/
trait DtoConvertor {
  def domainFromDescription(domainDescription:DomainDescription):Domain = {
    Domain(domainDescription.id, domainDescription.name, domainDescription.key, DomainMode.byName(domainDescription.mode), domainDescription.deleted)
  }
}