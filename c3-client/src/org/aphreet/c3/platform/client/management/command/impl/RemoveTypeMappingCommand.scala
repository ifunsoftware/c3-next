package org.aphreet.c3.platform.client.management.command.impl

class RemoveTypeMappingCommand extends Command{

  def execute:String = {
    
    if(params.size < 1){
      "Not enough params.\nUsage remove type mapping <mimetype>"
    }else{
      management.removeTypeMapping(params.first)
      "Type mapping removed"
    }
  }
  
  def name = List("remove", "type", "mapping")
}
