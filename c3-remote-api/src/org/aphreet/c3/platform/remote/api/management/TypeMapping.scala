package org.aphreet.c3.platform.remote.api.management

import reflect.BeanProperty

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 22, 2010
 * Time: 1:19:05 PM
 * To change this template use File | Settings | File Templates.
 */

class TypeMapping(
        @BeanProperty var mimeType:String,
        @BeanProperty var storage:String,
        @BeanProperty var versioned:java.lang.Boolean) extends java.io.Serializable{

  def this() = this(null, null, null)
}
