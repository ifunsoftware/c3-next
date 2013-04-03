package org.aphreet.c3.platform.storage.updater

import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.storage.Storage

/**
 * Copyright iFunSoftware 2011
 * @author Mikhail Malygin
 */

trait Transformation {

  def apply(storage: Storage, resource: Resource)

}