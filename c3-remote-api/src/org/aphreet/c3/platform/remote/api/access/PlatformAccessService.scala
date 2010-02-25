package org.aphreet.c3.platform.remote.api.access

import java.util.HashMap
/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 1:25:51 AM
 * To change this template use File | Settings | File Templates.
 */

trait PlatformAccessService{

  def getMetadata(ra:String):HashMap[String, String]

  def getResourceAsString(ra:String):String

}