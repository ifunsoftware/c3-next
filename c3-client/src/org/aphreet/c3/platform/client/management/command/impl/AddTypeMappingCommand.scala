package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.remote.rmi.management.RmiMimeTypeMapping

class AddTypeMappingCommand extends Command{
  
  def execute:String = {
    if(params.size < 2)
      "Not enough params.\nUsage: add type mapping <mimetype> <storagetype> <versioned>"
    else{
      
      val mimeType = params.first
      val storageType = params.tail.first
      val versioned = if(params(2) == "true") 1 else 0
      
      management.addTypeMapping(mimeType, storageType, versioned.shortValue)     
      
      "Type mapping added"
    }
  }
  
  def name:List[String] = List("add", "type", "mapping")

}
