package org.aphreet.c3.platform.remote.management.controllers.request

import scala.beans.BeanProperty

class StorageUpdateRequest(@BeanProperty var mode: String) {

  def this() = this(null)

}
