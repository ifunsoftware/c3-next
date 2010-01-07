package org.aphreet.c3.platform.storage.file

import org.aphreet.c3.platform.storage.common.AbstractStorageFactory
import org.springframework.stereotype.Component

@Component
class FileBDBStorageFactory extends AbstractStorageFactory{

  protected def createNewStorage(params:StorageParams):Storage = {
    
    val storage = new FileBDBStorage(params.id, params.path)
    storage.ids = params.secIds
    storage
    
  }
  
  def name:String = FileBDBStorage.NAME
  
}
