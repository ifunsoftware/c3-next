package org.aphreet.c3.platform.remote.management.model

import scala.beans.BeanProperty

case class ErrorModel(@BeanProperty var code: Int, @BeanProperty var message: String) {

  def this() = this(0, null)

}
