package org.aphreet.c3.platform.config

import java.util.{Map => JMap}
import collection.JavaConversions

trait SPlatformPropertyListener extends PlatformPropertyListener{

  def defaultValues:Map[String, String]
  
  override def defaultPropertyValues:JMap[String, String] = JavaConversions.asMap(defaultValues)

  override def listeningForProperties:Array[String] = defaultValues.keys.toArray
  
}
