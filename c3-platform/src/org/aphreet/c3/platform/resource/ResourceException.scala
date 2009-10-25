package org.aphreet.c3.platform.resource

class ResourceException(val msg:String, val cause:Throwable) extends RuntimeException(msg, cause){

  def this(msg:String) = this(msg, null)
  
  def this(cause:Throwable) = this(null, cause)
}

