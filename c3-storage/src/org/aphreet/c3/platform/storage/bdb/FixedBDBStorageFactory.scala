package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.storage.common.AbstractStorageFactory
import org.springframework.stereotype.Component

@Component
class FixedBDBStorageFactory extends AbstractStorageFactory{

  protected def createNewStorage(params:StorageParams):Storage = {
    
    val storage = new FixedBDBStorage(params.id, params.path)
    storage.secondaryIds = params.secIds
    storage
  }
  
  def name:String = FixedBDBStorage.NAME
}
