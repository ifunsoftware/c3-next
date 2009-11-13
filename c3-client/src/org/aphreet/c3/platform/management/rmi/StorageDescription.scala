package org.aphreet.c3.platform.management.rmi

class StorageDescription(val id:String, val storageType:String, val path:String, val mode:String) extends java.io.Serializable{
  
  override def toString:String = "[" + id + " " + storageType + "]"
  
}
