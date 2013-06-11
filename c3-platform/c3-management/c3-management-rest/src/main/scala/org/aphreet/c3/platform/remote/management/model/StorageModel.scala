package org.aphreet.c3.platform.remote.management.model

import scala.beans.BeanProperty
import org.aphreet.c3.platform.storage.Storage

case class StorageModel(
  @BeanProperty var id: String,
  @BeanProperty var storageType: String,
  @BeanProperty var path: String) {

  def this() = this(null, null, null)

}

object StorageModel{

  def apply(s: Storage): StorageModel = {
    StorageModel(s.params.id, s.params.storageType, s.params.path.stringValue)
  }

}
