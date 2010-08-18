package org.aphreet.c3.platform.management

import java.util.{Map => JMap}
import collection.JavaConversions

trait SPlatformPropertyListener extends PlatformPropertyListener{

  def defaultValues:Map[String, String]
  
  def defaultPropertyValues:JMap[String, String] = JavaConversions.asMap(defaultValues)
  
}
