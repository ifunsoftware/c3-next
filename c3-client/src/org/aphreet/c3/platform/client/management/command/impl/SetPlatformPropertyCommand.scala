package org.aphreet.c3.platform.client.management.command.impl

class SetPlatformPropertyCommand extends Command{

  def execute:String = {
    
    if(params.size < 2){
      "Not enought params\nUsage: set platform property <key> <value>" 
    }else{
      management.setPlatformProperty(params.first, params.tail.first)
      "Property set"
    }
    
  }
  
  def name:List[String] = List("set", "platform", "property")
}
