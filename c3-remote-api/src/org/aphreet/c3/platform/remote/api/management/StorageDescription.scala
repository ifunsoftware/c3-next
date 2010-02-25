package org.aphreet.c3.platform.remote.api.management

import reflect.BeanProperty

class StorageDescription(
        @BeanProperty var id:String,
        @BeanProperty var storageType:String,
        @BeanProperty var path:String,
        @BeanProperty var mode:String,
        @BeanProperty var count:java.lang.Long) extends java.io.Serializable{

  def this() = this(null, null, null, null, null)

  override def toString:String = {
    "[" + id + " " + storageType + "]"
  }
}

