package org.aphreet.c3.platform.client.management.command.impl

class AddSizeMappingCommand extends Command{

  def execute:String = {
    if(params.size < 2)
      "Not enough params.\nUsage: add size mapping <size> <storagetype> <versioned>"
    else{
      
      val size = params.first.toLong
      val storageType = params.tail.first
      val versioned = if(params(2) == "true") 1 else 0
      
      
      management.addSizeMapping(size, storageType, versioned)     
      
      "Size mapping added"
    }
  }
  
  def name = List("add", "size", "mapping")
}
