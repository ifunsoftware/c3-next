package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.storage.common.AbstractStorageFactory
import org.springframework.stereotype.Component

@Component
class PureBDBStorageFactory extends AbstractStorageFactory{

  protected def createNewStorage(params:StorageParams):Storage = {
    
    val storage = new PureBDBStorage(params.id, params.path)
    storage.ids = params.secIds
    storage
    
  }
  
  def name:String = PureBDBStorage.NAME
  
}
