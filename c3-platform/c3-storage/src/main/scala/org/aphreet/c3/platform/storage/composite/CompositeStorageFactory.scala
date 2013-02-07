package org.aphreet.c3.platform.storage.composite

import org.aphreet.c3.platform.storage.bdb.AbstractBDBStorageFactory
import org.aphreet.c3.platform.storage.{ConflictResolverProvider, Storage, StorageParams}
import org.springframework.stereotype.Component

@Component
class CompositeStorageFactory extends AbstractBDBStorageFactory{

  protected def createNewStorage(params:StorageParams, systemId:String, conflictResolverProvider: ConflictResolverProvider):Storage =
      new CompositeStorage(params, systemId, bdbConfig, conflictResolverProvider)

    def name:String = CompositeStorage.NAME
}
