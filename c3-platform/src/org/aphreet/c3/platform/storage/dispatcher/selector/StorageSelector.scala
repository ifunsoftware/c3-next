package org.aphreet.c3.platform.storage.dispatcher.selector

import org.aphreet.c3.platform.resource.Resource

trait StorageSelector {

  def storageTypeForResource(resource:Resource):(String, Boolean)
  
}
