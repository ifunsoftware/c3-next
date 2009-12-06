package org.aphreet.c3.platform.exception

class PropertyChangeException(override val message:String, override val cause:Throwable) extends ConfigurationException(message, cause){

  def this(message:String) = this(message, null)
  
  def this() = this(null, null)
  
  def this(cause:Throwable) = this(null, cause)
}