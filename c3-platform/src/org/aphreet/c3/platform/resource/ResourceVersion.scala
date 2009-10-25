package org.aphreet.c3.platform.resource

import java.util.Date

import scala.collection.mutable.{Map, HashMap}

class ResourceVersion{

  var date:Date = null
  
  var revision:Int = 0
  
  var systemMetadata:Map[String, String] = new HashMap
  
  var data:DataWrapper = null
  
  override def toString:String = {
    date + " " + revision + " " + systemMetadata 
  }

}
