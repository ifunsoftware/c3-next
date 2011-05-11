/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright 
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above 
 * copyright notice, this list of conditions and the following disclaimer 
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors 
 * may be used to endorse or promote products derived from this software 
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.{Command, Commands}
import org.aphreet.c3.platform.remote.api.management.RemoteTaskDescription

object TaskCommands extends Commands{

  def instances = List(
    new ListRunningTasksCommand,
    new ListFinishedTasksCommand,
    new SetTaskMode
  )
}

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

  def getTasks:Array[RemoteTaskDescription]

}

class ListRunningTasksCommand extends ListTasksCommand {

  override def getTasks:Array[RemoteTaskDescription] = management.listTasks

  override def name = List("list", "tasks")

}

class ListFinishedTasksCommand extends ListTasksCommand {

  override def getTasks:Array[RemoteTaskDescription] = management.listFinishedTasks

  override def name = List("list", "finished", "tasks")

}

class SetTaskMode extends Command {

  def execute:String = {

    if(params.size < 2){
      wrongParameters("set task mode <id> <pause|resume>")
    }else{
      management.setTaskMode(params.head, params.tail.head )
      "Resumed"
    }
  }

  def name = List("set", "task", "mode")

}
