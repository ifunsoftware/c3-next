package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.resource.Resource

trait ConflictResolver {

  def resolve(resource: Resource, savedResource: Resource): Resource

}
