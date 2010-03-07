package org.aphreet.c3.platform.exception

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 7, 2010
 * Time: 5:26:42 PM
 * To change this template use File | Settings | File Templates.
 */

class StorageNotFoundException(override val message:String, override val cause:Throwable) extends PlatformException(message, cause){

  def this(message:String) = this(message, null)

  def this() = this(null, null)

  def this(cause:Throwable) = this(null, cause)

}