package org.aphreet.c3.platform.remote.management.model

import scala.beans.BeanProperty
import org.aphreet.c3.platform.storage.{StorageIndex, Storage}

case class StorageModel(
  @BeanProperty var id: String,
  @BeanProperty var storageType: String,
  @BeanProperty var path: String,
  @BeanProperty var mode: String,
  @BeanProperty var indexes: Array[StorageIndexModel]) {

  def this() = this(null, null, null, null, null)

}

object StorageModel{

  def apply(s: Storage): StorageModel = {
    StorageModel(s.params.id, s.params.storageType, s.params.path.stringValue,
      s.mode.toString, s.params.indexes.map(StorageIndexModel(_)).toArray)
  }

}

case class StorageIndexModel(
  @BeanProperty var name: String,
  @BeanProperty var fields: Array[String],
  @BeanProperty var multi: Boolean,
  @BeanProperty var system: Boolean,
  @BeanProperty var created: Long){

  def this() = this(null, null, false, false, 0l)

}

object StorageIndexModel{

  def apply(index: StorageIndex): StorageIndexModel =
    StorageIndexModel(index.name, index.fields.toArray, index.multi, index.system, index.created)

}

