package org.aphreet.c3.platform.exception

class MigrationException(override val message:String, override val cause:Throwable) extends PlatformException(message, cause){

  def this(message:String) = this(message, null)
  
  def this() = this(null, null)
  
  def this(cause:Throwable) = this(null, cause)
}
