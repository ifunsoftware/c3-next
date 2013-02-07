package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.resource.Resource

class DefaultConflictResolver extends ConflictResolver{

  def resolve(savedResource: Resource, resource: Resource) {
    savedResource.addVersion(resource.versions.last)
  }

}
