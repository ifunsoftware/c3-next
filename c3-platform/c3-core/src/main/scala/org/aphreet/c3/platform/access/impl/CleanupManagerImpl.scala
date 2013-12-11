package org.aphreet.c3.platform.access.impl

import org.aphreet.c3.platform.access.{ResourceDeletedMsg, AccessMediator, CleanupManager}
import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.storage.Storage
import org.aphreet.c3.platform.storage.updater.{StorageUpdater, Transformation}

class CleanupManagerImpl(val storageUpdater: StorageUpdater,
                         val accessMediator: AccessMediator) extends CleanupManager{

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
