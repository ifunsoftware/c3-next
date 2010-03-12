package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.exception.StorageException

abstract sealed class StorageMode(val name:String, val message:String){
  
  def allowWrite:Boolean
  
  def allowRead:Boolean

  override def toString = name + "(" + message + ")"
  
}

case class RW(val msg:String) extends StorageMode("RW", msg){
  
  def this() = this(STORAGE_MODE_NONE)
  
  def allowWrite = true
  
  def allowRead = true
  
}

case class RO(val msg:String) extends StorageMode("RO", msg){
  
  def this() = this(STORAGE_MODE_NONE)
  
  def allowWrite = false
  
  def allowRead = true
  
}

case class U(val msg:String) extends StorageMode("U", msg){
  
  def this() = this(STORAGE_MODE_NONE)
  
  def allowWrite = false
  
  def allowRead = false
}


object StorageModeParser{
	
  def valueOf(name:String):StorageMode = valueOf(name, "")
  
  def valueOf(name:String, message:String):StorageMode = {
    name match {
      case "RW" => RW(message)
      case "RO" => RO(message)
      case "U" => U(message)
      case _ => throw new StorageException("No such mode " + name)
    }
  }
}