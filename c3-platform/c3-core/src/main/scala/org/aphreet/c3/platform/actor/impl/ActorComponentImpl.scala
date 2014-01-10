package org.aphreet.c3.platform.actor.impl

import org.aphreet.c3.platform.actor.ActorComponent
import akka.actor.ActorSystem

/**
 * Author: Mikhail Malygin
 * Date:   12/27/13
 * Time:   4:21 PM
 */
trait ActorComponentImpl extends ActorComponent{

  val actorSystem = ActorSystem.apply()
  
}
