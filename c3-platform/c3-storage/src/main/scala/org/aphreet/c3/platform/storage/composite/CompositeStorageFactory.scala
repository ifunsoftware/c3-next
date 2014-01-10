package org.aphreet.c3.platform.storage.composite

import akka.actor.ActorRefFactory
import org.aphreet.c3.platform.actor.ActorComponent
import org.aphreet.c3.platform.common.ComponentLifecycle
import org.aphreet.c3.platform.config.{PlatformConfigComponent, PlatformConfigManager}
import org.aphreet.c3.platform.storage.StorageParams
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.storage.bdb.AbstractBDBStorageFactory

trait CompositeStorageComponent extends ComponentLifecycle{

  this: StorageComponent
    with PlatformConfigComponent
    with ActorComponent =>

  val compositeStorageFactory = new CompositeStorageFactory(storageManager, platformConfigManager, actorSystem)

  destroy(Unit => compositeStorageFactory.destroy())

}

class CompositeStorageFactory(override val storageManager: StorageManager,
                              override val platformConfigManager: PlatformConfigManager,
                              override val actorSystem: ActorRefFactory)
  extends AbstractBDBStorageFactory(storageManager, platformConfigManager, actorSystem){

  protected def createNewStorage(params:StorageParams, systemId:String, conflictResolverProvider: ConflictResolverProvider):Storage =
      new CompositeStorage(params, systemId, bdbConfig, conflictResolverProvider)

    def name:String = CompositeStorage.NAME
}
