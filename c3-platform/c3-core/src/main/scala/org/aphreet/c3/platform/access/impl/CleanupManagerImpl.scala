package org.aphreet.c3.platform.access.impl

import org.aphreet.c3.platform.access.{ResourceDeletedMsg, AccessMediator, CleanupManager}
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.storage.Storage
import org.aphreet.c3.platform.storage.updater.{StorageUpdater, Transformation}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.common.Logger

@Component("cleanupManager")
class CleanupManagerImpl extends CleanupManager{

  @Autowired
  var storageUpdater: StorageUpdater = _

  @Autowired
  var accessMediator: AccessMediator = _

  def cleanupResources(filter: Resource => Boolean) {
    storageUpdater.applyTransformation(new CleanupTransformation(filter, accessMediator))
  }
}

class CleanupTransformation(val filter: Resource => Boolean, val accessMediator: AccessMediator) extends Transformation{

  val log = Logger(getClass)

  def apply(storage: Storage, resource: Resource) {
    if(filter(resource)){

      log.info("Deleting resource " + resource.address)

      storage.delete(resource.address)
      accessMediator ! ResourceDeletedMsg(resource.address, 'CleanupManager)
    }
  }

}
