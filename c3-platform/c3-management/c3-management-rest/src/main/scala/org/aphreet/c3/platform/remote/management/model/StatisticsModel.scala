package org.aphreet.c3.platform.remote.management.model

import scala.beans.BeanProperty
import java.util

case class StatisticsModel(@BeanProperty var statistics: util.Map[String, String]) {

  def this() = this(null)

}