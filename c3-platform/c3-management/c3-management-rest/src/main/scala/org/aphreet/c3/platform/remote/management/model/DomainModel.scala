package org.aphreet.c3.platform.remote.management.model

import scala.beans.BeanProperty
import org.aphreet.c3.platform.domain.Domain

case class DomainModel(
  @BeanProperty var id: String,
  @BeanProperty var name: String,
  @BeanProperty var key: String,
  @BeanProperty var mode: String,
  @BeanProperty var default: Boolean) {

  def this() = this(null, null, null, null, false)
}

object DomainModel{
  def apply(domain: Domain, defaultDomainId: String): DomainModel = {
    DomainModel(domain.id, domain.name, domain.key, domain.mode.name, domain.id == defaultDomainId)
  }
}
