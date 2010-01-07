package org.aphreet.c3.platform.management

import java.util.{Map => JMap}
import scala.collection.jcl.HashMap

trait SPlatformPropertyListener extends PlatformPropertyListener{

  def defaultValues:Map[String, String]
  
  def defaultPropertyValues:JMap[String, String] = {
    val result = new HashMap[String, String]
    result ++ defaultValues
    result.underlying
  }
  
}
