package org.aphreet.c3.platform.client.command

import org.aphreet.c3.platform.management.rmi.RmiTaskDescr

class ListTasksCommand extends Command {

  def execute:String = {
    
    val tasks = management.listTasks
    
    val builder = new StringBuilder
    
    for(t:RmiTaskDescr <- tasks){
      
      val progress = t.progress match {
        case "-1" => "N/A"
        case _ => t.progress
      }
      
      builder.append(String.format("%-50s %-30s %-10s %s\n", t.id, t.name, t.status, progress))
    }
    
    builder.toString
    
  }
  
  def name = List("list", "tasks")
}
