package org.aphreet.c3.platform.remote.management.controllers.request

import scala.beans.BeanProperty

case class DomainUpdateRequest(
  @BeanProperty var domainName: String,
  @BeanProperty var domainMode: String,
  @BeanProperty var keyAction: String,
  @BeanProperty var default: String) {

  def needsNameUpdate(): Boolean = !domainName.isEmpty

  def needsModeUpdate(): Boolean = !domainMode.isEmpty

  def needsKeyAction():  Boolean = !keyAction.isEmpty

  def isDefaultDomain: Boolean = "true" == default
}
