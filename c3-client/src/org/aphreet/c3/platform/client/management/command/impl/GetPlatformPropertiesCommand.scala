package org.aphreet.c3.platform.client.management.command.impl

import scala.collection.jcl.HashMap

class GetPlatformPropertiesCommand extends Command{

  def execute:String = {
    val map:HashMap[String, String] = new HashMap(management.platformProperties)
    
    map.map(e => e._1 + "=" + e._2 + "\n").foldLeft("")(_ + _)
  }
  
  def name:List[String] = List("list", "platform", "properties")
}
