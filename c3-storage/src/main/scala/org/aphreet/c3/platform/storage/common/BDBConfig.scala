package org.aphreet.c3.platform.storage.common

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 11, 2010
 * Time: 1:08:29 AM
 * To change this template use File | Settings | File Templates.
 */

class BDBConfig(val txNoSync:Boolean,
                val txWriteNoSync:Boolean,
                val cachePercent:Int){

  override def toString =
    "[txNoSync:" + txNoSync + ", txWriteNoSync:" + txWriteNoSync + ", cachePercent:" + cachePercent +"]"
}
