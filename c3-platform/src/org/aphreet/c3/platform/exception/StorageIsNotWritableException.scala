package org.aphreet.c3.platform.exception

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 8, 2010
 * Time: 9:31:12 PM
 * To change this template use File | Settings | File Templates.
 */

class StorageIsNotWritableException(override val message:String, override val cause:Throwable) extends PlatformException(message, cause){

  def this(message:String) = this(message, null)

  def this() = this(null, null)

  def this(cause:Throwable) = this(null, cause)

}