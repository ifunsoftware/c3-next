package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.resource.Resource

trait ConflictResolverProvider {

  def conflictResolverFor(resource: Resource): ConflictResolver

}
