package org.aphreet.c3.platform.config

import java.util.{Map => JMap}
import collection.JavaConversions._

trait SPlatformPropertyListener extends PlatformPropertyListener{

  def defaultValues:Map[String, String]
  
  override def defaultPropertyValues:JMap[String, String] = mapAsJavaMap(defaultValues)

  override def listeningForProperties:Array[String] = defaultValues.keys.toArray
  
}
