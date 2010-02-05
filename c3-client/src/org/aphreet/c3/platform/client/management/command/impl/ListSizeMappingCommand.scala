package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.remote.rmi.management.RmiSizeMapping

class ListSizeMappingCommand extends Command{

  def execute:String = {
    val builder = new StringBuilder
    
    for(mapping:RmiSizeMapping <- management.listSizeMappings)
      builder.append(String.format("%10d %20s %d\n", mapping.size, mapping.storage, mapping.versioned))
    
    
    builder.toString  
  }
  
  def name = List("list", "size", "mappings")
}
