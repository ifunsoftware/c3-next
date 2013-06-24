package org.aphreet.c3.platform.remote.management.model

import java.util
import scala.beans.BeanProperty

case class PropertiesModel(@BeanProperty var properties: util.Map[String, String]) {

  def this() = this(null)

}
