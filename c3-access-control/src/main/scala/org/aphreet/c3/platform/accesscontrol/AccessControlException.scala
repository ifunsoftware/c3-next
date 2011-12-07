package org.aphreet.c3.platform.accesscontrol

import org.aphreet.c3.platform.exception.PlatformException

/**
 * Copyright iFunSoftware 2011
 * @author Mikhail Malygin
 */

class AccessControlException(override val message:String, override val cause:Throwable) extends PlatformException(message, cause){

  def this(message:String) = this(message, null)

  def this() = this(null, null)

  def this(cause:Throwable) = this(null, cause)

}