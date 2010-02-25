package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.remote.api.management.Pair
import org.aphreet.c3.platform.client.management.command.Command

class GetPlatformPropertiesCommand extends Command{

  def execute:String = {
    val map:Array[Pair] = management.platformProperties
    
    map.map(e => e.key + "=" + e.value + "\n").foldLeft("")(_ + _)
  }
  
  def name:List[String] = List("list", "platform", "properties")
}
