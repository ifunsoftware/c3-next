package org.aphreet.c3.platform.remote.management.model

import scala.beans.BeanProperty
import org.aphreet.c3.platform.auth.User

case class UserModel(
  @BeanProperty var name: String,
  @BeanProperty var enabled: Boolean) {

  def this() = this(null, false)
}

object UserModel{

  def apply(user: User): UserModel = UserModel(user.name, user.enabled)

}
