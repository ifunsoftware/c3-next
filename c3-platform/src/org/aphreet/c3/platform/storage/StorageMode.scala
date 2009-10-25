package org.aphreet.c3.platform.storage


object RW extends StorageMode("RW")
object RO extends StorageMode("RO")
object U  extends StorageMode("U")
object USER_RO extends StorageMode("USER_RO")
object USER_U extends StorageMode("USER_U")


sealed class StorageMode(val name:String);

object StorageModeParser{
	
  def valueOf(name:String):StorageMode = {
    name match {
      case "RW" => RW
      case "RO" => RO
      case "U" => U
      case "USER_RO" => USER_RO
      case "USER_U" => USER_U
      case _ => throw new StorageException("No such mode " + name)
    }
  }
}