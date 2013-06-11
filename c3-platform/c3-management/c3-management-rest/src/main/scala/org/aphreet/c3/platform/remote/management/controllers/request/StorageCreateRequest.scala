package org.aphreet.c3.platform.remote.management.controllers.request

import scala.beans.BeanProperty

class StorageCreateRequest(
    @BeanProperty var storageType: String,
    @BeanProperty var path: String){

    def this() = this(null, null)
  }
