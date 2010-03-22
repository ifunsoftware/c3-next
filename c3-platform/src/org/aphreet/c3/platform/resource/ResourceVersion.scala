package org.aphreet.c3.platform.resource

import java.util.Date

import scala.collection.jcl.{Map, HashMap}

import java.util.{Map => JMap}

class ResourceVersion{

  val RESOURCE_VERSION_HASH = "c3.data.md5"

  var date:Date = new Date
  
  var revision:Int = 0
  
  var systemMetadata:HashMap[String, String] = new HashMap
  
  var data:DataWrapper = null
  
  var persisted = false;
  
  override def toString:String = {
    date + " " + data.length + " " + revision + " " + systemMetadata 
  }
  
  def getMetadata:JMap[String, String] = systemMetadata.underlying

  def setData(_data:DataWrapper) = {data = _data}

  def calculateHash = {
    systemMetadata.put(RESOURCE_VERSION_HASH, data.hash)
  }

}
