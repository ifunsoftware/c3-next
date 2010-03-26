package org.aphreet.c3.platform.storage.dispatcher

import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.storage.Storage

trait StorageDispatcher{
  
  def setStorages(sts:List[Storage])
  
  def selectStorageForResource(resource:Resource):Storage
  
}
