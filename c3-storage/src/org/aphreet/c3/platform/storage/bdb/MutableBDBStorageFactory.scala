package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.storage.common.AbstractStorageFactory
import org.springframework.stereotype.Component

@Component
class MutableBDBStorageFactory extends AbstractStorageFactory{

  protected def createNewStorage(params:StorageParams):Storage = {
    
    val storage = new MutableBDBStorage(params.id, params.path)
    storage.secondaryIds = params.secIds
    storage
    
  }
  
  def name:String = MutableBDBStorage.NAME
}


