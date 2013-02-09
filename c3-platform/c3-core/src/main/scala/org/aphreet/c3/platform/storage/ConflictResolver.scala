package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.resource.Resource

trait ConflictResolver {

  def resolve(savedResource: Resource, resource: Resource)

}
