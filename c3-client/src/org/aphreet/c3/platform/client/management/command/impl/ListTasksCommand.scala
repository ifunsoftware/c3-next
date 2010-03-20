package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.Command
import org.aphreet.c3.platform.remote.api.management.TaskDescription

abstract class ListTasksCommand extends Command {

  def execute:String = {
    
    val tasks = getTasks
    
    val builder = new StringBuilder

    builder.append(String.format("%-40s %-25s %-10s %s\n", "Task Id", "Task name", "Status", "Progress"))


    for(t <- tasks){
      
      val progress = t.progress match {
        case "-1" => "N/A"
        case _ => t.progress
      }
      
      builder.append(String.format("%-40s %-25s %-10s %s\n", t.id, t.name, t.status, progress))
    }
    
    builder.toString
    
  }

  def getTasks:Array[TaskDescription]

}

class ListRunningTasksCommand extends ListTasksCommand {

  override def getTasks:Array[TaskDescription] = management.listTasks

  override def name = List("list", "tasks")

}

class ListFinishedTasksCommand extends ListTasksCommand {

  override def getTasks:Array[TaskDescription] = management.listFinishedTasks

  override def name = List("list", "finished", "tasks")

}