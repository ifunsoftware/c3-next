package org.aphreet.c3.platform.resource

import java.util.Date

import scala.collection.mutable.HashMap

class ResourceVersion{

  val RESOURCE_VERSION_HASH = "c3.data.md5"

  var date:Date = new Date
  
  var revision:Int = 0
  
  var systemMetadata = new HashMap[String, String]
  
  var data:DataWrapper = null
  
  var persisted = false;
  
  override def toString:String = {
    val builder = new StringBuilder

    builder.append(date.toString).append(" ").append(data.length).append(" ").append(revision)
    builder.append("\n\tMetadata:")

    for((key, value) <- systemMetadata){
      builder.append("\n\t\t").append(key).append(" => ").append(value)
    }

    builder.toString
  }

  def setData(_data:DataWrapper) = {data = _data}

  def calculateHash = {
    systemMetadata.put(RESOURCE_VERSION_HASH, data.hash)
  }

}
