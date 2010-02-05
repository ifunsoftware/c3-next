package org.aphreet.c3.platform.client.management.command.impl

class ShowResourceCommand extends Command {

  def execute:String = {
    
    if(params.length < 1){
      "Not enought params.\nUsage: show resource <address>"
    }else{
      val result = access.getResourceAsString(params.head)
      if(result != null){
        result
      }else{
        "Resource not found"
      }
    }
  }
  
  def name:List[String] = List("show", "resource")
}
