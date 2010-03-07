package org.aphreet.c3.platform.exception

import org.aphreet.c3.platform.storage.StorageException

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 7, 2010
 * Time: 5:27:17 PM
 * To change this template use File | Settings | File Templates.
 */

class ResourceNotFoundException(override val message:String, override val cause:Throwable) extends StorageException(message, cause){

  def this(message:String) = this(message, null)

  def this() = this(null, null)

  def this(cause:Throwable) = this(null, cause)

}