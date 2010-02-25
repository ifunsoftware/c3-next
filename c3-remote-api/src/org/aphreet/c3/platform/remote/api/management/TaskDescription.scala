package org.aphreet.c3.platform.remote.api.management

import reflect.BeanProperty

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 22, 2010
 * Time: 1:16:28 PM
 * To change this template use File | Settings | File Templates.
 */

class TaskDescription(
        @BeanProperty var id:String,
        @BeanProperty var name:String,
        @BeanProperty var status:String,
        @BeanProperty var progress:String) extends java.io.Serializable{

  def this() = this(null, null, null, null)
}
