package org.aphreet.c3.platform.remote.api.management

import reflect.BeanProperty

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 25, 2010
 * Time: 10:58:00 PM
 * To change this template use File | Settings | File Templates.
 */

class Pair(
    @BeanProperty var key:String,
    @BeanProperty var value:String) extends java.io.Serializable{

  def this() = this(null, null)
  
}