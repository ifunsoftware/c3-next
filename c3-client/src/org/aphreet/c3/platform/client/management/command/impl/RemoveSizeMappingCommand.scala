package org.aphreet.c3.platform.client.management.command.impl

class RemoveSizeMappingCommand extends Command{

  def execute:String = {
	
    if(params.size < 1){
      "Not enough params.\nUsage remove size mapping <size>"
    }else{
      management.removeSizeMapping(params.first.toLong)
      "Size mapping removed"
    }
  }
  
  def name = List("remove", "size", "mapping")
  
}
