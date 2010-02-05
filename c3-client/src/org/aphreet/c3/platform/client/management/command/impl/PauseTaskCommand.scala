package org.aphreet.c3.platform.client.management.command.impl

class PauseTaskCommand extends Command{

  def execute:String = {
    
    if(params.size < 1){
      "Not enought params.\nUsage: pause task <id>"
    }else{
      management.setTaskMode(params.first, "pause" )
      "Paused"
    }
    
    
  }
  
  def name = List("pause", "task")
}