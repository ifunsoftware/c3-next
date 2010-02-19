package org.aphreet.c3.platform.remote.api.rmi.management

class StorageDescription(val id:String, val storageType:String, val path:String, val mode:String, val count:java.lang.Long) extends java.io.Serializable{
  
  override def toString:String = {
    "[" + id + " " + storageType + "]"
  }
}

