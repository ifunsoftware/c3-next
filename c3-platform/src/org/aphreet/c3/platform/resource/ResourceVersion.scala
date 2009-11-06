package org.aphreet.c3.platform.resource

import java.util.Date

import scala.collection.jcl.{Map, HashMap}

import java.util.{Map => JMap}

class ResourceVersion{

  var date:Date = new Date
  
  var revision:Int = 0
  
  var systemMetadata:HashMap[String, String] = new HashMap
  
  var data:DataWrapper = null
  
  var persisted = false;
  
  override def toString:String = {
    date + " " + revision + " " + systemMetadata 
  }
  
  def getMetadata:JMap[String, String] = systemMetadata.underlying

}
