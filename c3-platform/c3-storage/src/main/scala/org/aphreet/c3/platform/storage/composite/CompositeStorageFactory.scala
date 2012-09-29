package org.aphreet.c3.platform.storage.composite

import org.aphreet.c3.platform.storage.bdb.AbstractBDBStorageFactory
import org.aphreet.c3.platform.storage.{Storage, StorageParams}
import org.springframework.stereotype.Component

@Component
class CompositeStorageFactory extends AbstractBDBStorageFactory{

  protected def createNewStorage(params:StorageParams, systemId:String):Storage =
      new CompositeStorage(params, systemId, bdbConfig)

    def name:String = CompositeStorage.NAME
}
