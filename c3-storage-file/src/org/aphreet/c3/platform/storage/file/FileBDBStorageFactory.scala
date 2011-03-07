package org.aphreet.c3.platform.storage.file

import org.springframework.stereotype.Component
import org.aphreet.c3.platform.storage.{Storage, StorageParams}
import org.aphreet.c3.platform.storage.common.AbstractBDBStorageFactory

@Component
class FileBDBStorageFactory extends AbstractBDBStorageFactory{

  protected def createNewStorage(params:StorageParams, systemId:String):Storage = {
    
    val storage = new FileBDBStorage(params, systemId, bdbConfig)
    storage.ids = params.secIds
    storage
    
  }
  
  def name:String = FileBDBStorage.NAME
  
}
