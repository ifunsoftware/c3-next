package org.aphreet.c3.platform.actor

import akka.actor.ActorRefFactory

/**
 * Author: Mikhail Malygin
 * Date:   12/27/13
 * Time:   4:20 PM
 */
trait ActorComponent {

  def actorSystem: ActorRefFactory

}
