package org.aphreet.c3.platform.access

import org.aphreet.c3.platform.resource.Resource

trait CleanupManager {

  def cleanupResources(filter: Resource => Boolean)

}
