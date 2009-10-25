package org.aphreet.c3.platform.storage.file

import org.aphreet.c3.platform.storage.common.AbstractStorageFactory
import org.springframework.stereotype.Component

@Component
class FileStorageFactory extends AbstractStorageFactory{

   protected def createNewStorage(params:StorageParams):Storage = {
    
    val storage = new FileStorage(params.id, params.path)
    storage.secondaryIds = params.secIds
    storage
  }
  
  def name:String = FileStorage.NAME
}
