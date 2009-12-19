package org.aphreet.c3.platform.client.command

import org.aphreet.c3.platform.management.rmi.RmiTaskDescr

class ListTasksCommand extends Command {

  def execute:String = {
    
    val tasks = management.listTasks
    
    val builder = new StringBuilder
    
    for(t:RmiTaskDescr <- tasks){
      
      builder.append(String.format("%-50s %-30s %-10s %d%%\n", t.id, t.name, t.status, t.progress))
    }
    
    builder.toString
    
  }
  
  def name = List("list", "tasks")
}
