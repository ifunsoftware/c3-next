package org.aphreet.c3.platform.remote.api

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 10:41:36 PM
 * To change this template use File | Settings | File Templates.
 */

class RemoteException(val message:String, val cause:Throwable) extends RuntimeException(message, cause){

  def this(message:String) = this(message, null)

  def this() = this(null, null)

  def this(cause:Throwable) = this(null, cause)
}