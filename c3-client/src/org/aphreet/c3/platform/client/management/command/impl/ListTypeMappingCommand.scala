package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.Command

class ListTypeMappingCommand extends Command{
  
  def execute:String = {
    
    val builder = new StringBuilder
    
    for(mapping <- management.listTypeMappigs)
      builder.append(String.format("%20s %20s %d\n", mapping.mimeType, mapping.storage, mapping.versioned))
    
    
    builder.toString
    
  }
  
  def name:List[String] = List("list", "type", "mappings")
  

}
