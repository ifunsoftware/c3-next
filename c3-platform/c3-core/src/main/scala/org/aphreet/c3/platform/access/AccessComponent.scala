package org.aphreet.c3.platform.access

import org.aphreet.c3.platform.resource.Resource

/**
 * Author: Mikhail Malygin
 * Date:   12/11/13
 * Time:   12:33 AM
 */
trait AccessComponent {

  def accessManager: AccessManager

  def accessMediator: AccessMediator

}

object AccessComponent {
  val accessMediatorName = "AccessMediator"
}

trait CleanupComponent {

  def cleanupManager: CleanupManager

}

case class ResourceAddedMsg(resource:Resource, source:Symbol)

case class ResourceUpdatedMsg(resource:Resource, source:Symbol)

case class ResourceDeletedMsg(address:String, source:Symbol)