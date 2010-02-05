package org.aphreet.c3.platform.client.management.command.impl

class ResumeTaskCommand extends Command{

  def execute:String = {
    
    if(params.size < 1){
      "Not enought params.\nUsage: resume task <id>"
    }else{
      management.setTaskMode(params.first, "resume" )
      "Resumed"
    }
    
    
  }
  
  def name = List("resume", "task")
}
