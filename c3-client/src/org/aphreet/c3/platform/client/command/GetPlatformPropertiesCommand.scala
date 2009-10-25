package org.aphreet.c3.platform.client.command

import scala.collection.mutable.HashMap

class GetPlatformPropertiesCommand extends Command{

  def execute:String = {
    val map:HashMap[String, String] = management.platformProperties
    
    map.map(e => e._1 + "=" + e._2 + "\n").foldLeft("")(_ + _)
  }
  
  def name:List[String] = List("list", "platform", "properties")
}
