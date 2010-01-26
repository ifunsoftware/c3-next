package org.aphreet.c3.platform.client.command

class ListTypeMappingCommand extends Command{
  
  def execute:String = {
    
    val builder = new StringBuilder
    
    for(mapping <- management.listTypeMappigs)
      builder.append(String.format("%20s %20s %d", mapping.mimeType, mapping.storage, mapping.versioned))
    
    
    builder.toString
    
  }
  
  def name:List[String] = List("list", "type", "mappings")
  

}
