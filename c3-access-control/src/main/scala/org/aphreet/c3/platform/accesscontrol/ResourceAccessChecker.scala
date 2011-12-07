package org.aphreet.c3.platform.accesscontrol

import org.aphreet.c3.platform.resource.Resource

/**
 * Copyright iFunSoftware 2011
 * @author Mikhail Malygin
 */

trait ResourceAccessChecker {

  def canPerformActionWithResource(action:Action, resource:Resource, accessParams:Map[String, String]):Boolean

}