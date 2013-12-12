package org.aphreet.c3.platform.storage.composite

import org.aphreet.c3.platform.storage.bdb.AbstractBDBStorageFactory
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.config.{PlatformConfigComponent, PlatformConfigManager}
import org.aphreet.c3.platform.common.ComponentLifecycle
import org.aphreet.c3.platform.storage.StorageParams

trait CompositeStorageComponent extends ComponentLifecycle{

  this: StorageComponent
    with PlatformConfigComponent =>

  val compositeStorageFactory = new CompositeStorageFactory(storageManager, platformConfigManager)

  destroy(Unit => compositeStorageFactory.destroy())

}

class CompositeStorageFactory(override val storageManager: StorageManager,
                              override val platformConfigManager: PlatformConfigManager)
  extends AbstractBDBStorageFactory(storageManager, platformConfigManager){

  protected def createNewStorage(params:StorageParams, systemId:String, conflictResolverProvider: ConflictResolverProvider):Storage =
      new CompositeStorage(params, systemId, bdbConfig, conflictResolverProvider)

    def name:String = CompositeStorage.NAME
}
