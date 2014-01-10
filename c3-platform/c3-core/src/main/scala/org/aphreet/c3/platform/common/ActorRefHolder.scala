package org.aphreet.c3.platform.common

import akka.actor.ActorRef

/**
 * Author: Mikhail Malygin
 * Date:   1/2/14
 * Time:   6:26 PM
 */
trait ActorRefHolder {

  def async: ActorRef

  def !(message : Any): Unit = async ! message
}
