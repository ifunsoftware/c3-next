package org.aphreet.c3.platform.remote.management.controllers.request

import scala.beans.BeanProperty

case class UserUpdateRequest(
  @BeanProperty var name: String,
  @BeanProperty var password: String,
  @BeanProperty var enabled: Boolean){

}
