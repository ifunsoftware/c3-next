package org.aphreet.c3.platform.storage.bdb

import org.springframework.stereotype.Component
import org.aphreet.c3.platform.storage.{Storage, StorageParams}
import org.aphreet.c3.platform.storage.common.AbstractBDBStorageFactory

@Component
class PureBDBStorageFactory extends AbstractBDBStorageFactory{

  protected def createNewStorage(params:StorageParams, systemId:Int):Storage = {
    
    val storage = new PureBDBStorage(params, systemId, bdbConfig)
    storage.ids = params.secIds
    storage
    
  }
  
  def name:String = PureBDBStorage.NAME
  
}
