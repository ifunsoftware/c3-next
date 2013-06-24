package org.aphreet.c3.platform.remote.management.controllers.request

import scala.beans.BeanProperty

case class PropertyUpdateRequest(
  @BeanProperty var key: String,
  @BeanProperty var value: String) {

  def this() = this(null, null)
}
