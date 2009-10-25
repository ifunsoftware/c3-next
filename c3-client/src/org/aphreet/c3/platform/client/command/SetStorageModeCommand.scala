package org.aphreet.c3.platform.client.command

class SetStorageModeCommand extends Command{

  def execute:String = {
    
    if(params.size < 2){
      "Not enought params.\nUsage: set storage mode <id> <mode>"
    }else{
      management.setStorageMode(params.first, params.tail.first)
      "Mode set"
    }
    
    
  }
  
  def name = List("set", "storage", "mode")
}
