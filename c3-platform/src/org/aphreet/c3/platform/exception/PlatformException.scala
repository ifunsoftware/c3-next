package org.aphreet.c3.platform.exception

class PlatformException(val message:String, val cause:Throwable) extends RuntimeException(message, cause){

  def this(message:String) = this(message, null)
  
  def this() = this(null, null)
  
  def this(cause:Throwable) = this(null, cause)
}
