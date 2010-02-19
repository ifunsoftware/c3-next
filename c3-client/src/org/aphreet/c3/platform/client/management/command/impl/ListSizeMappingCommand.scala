package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.Command

class ListSizeMappingCommand extends Command{

  def execute:String = {
    val builder = new StringBuilder
    
    for(mapping <- management.listSizeMappings)
      builder.append(String.format("%10d %20s %d\n", mapping.size, mapping.storage, mapping.versioned))
    
    
    builder.toString  
  }
  
  def name = List("list", "size", "mappings")
}
