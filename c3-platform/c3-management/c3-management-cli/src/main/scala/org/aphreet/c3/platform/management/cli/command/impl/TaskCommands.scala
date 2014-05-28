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

package org.aphreet.c3.platform.management.cli.command.impl

import org.aphreet.c3.platform.management.cli.command.{Command, Commands}
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.task.{RUNNING, PAUSED, TaskState, TaskDescription}

object TaskCommands extends Commands {

  def instances = List(
    new ListRunningTasksCommand,
    new ListFinishedTasksCommand,
    new SetTaskMode,
    new ListScheduledTasksCommand,
    new RescheduleTaskCommand,
    new RemoveScheduledTaskCommand
  )
}

abstract class ListTasksCommand extends Command {

  override
  def execute(management: PlatformManagementService): String = {

    val tasks = getTasks(management)

    val builder = new StringBuilder

    builder.append(String.format("%-40s %-25s %-10s %s\n", "Task Id", "Task name", "Status", "Progress"))


    for (t <- tasks) {

      val progress = t.progress match {
        case -1 => "N/A"
        case _ => t.progress.toString
      }

      builder.append(String.format("%-40s %-25s %-10s %s\n", t.id, t.name, t.state.name, progress))
    }

    builder.toString()

  }

  def getTasks(management: PlatformManagementService): List[TaskDescription]

}

class ListRunningTasksCommand extends ListTasksCommand {

  override
  def getTasks(management: PlatformManagementService) = management.coreManagement.listTasks

  override
  def name = List("list", "tasks")

}

class ListFinishedTasksCommand extends ListTasksCommand {

  override
  def getTasks(management: PlatformManagementService) = management.coreManagement.listFinishedTasks

  override
  def name = List("list", "finished", "tasks")
}

class ListScheduledTasksCommand extends ListTasksCommand {

  override
  def getTasks(management: PlatformManagementService) = management.coreManagement.listScheduledTasks

  override
  def name = List("list", "scheduled", "tasks")
}

class SetTaskMode extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {

    if (params.size < 2) {
      wrongParameters("set task mode <id> <pause|resume>")
    } else {

      val state: TaskState = params.tail.head match {
        case "pause" => PAUSED
        case "resume" => RUNNING
        case _ => throw new IllegalArgumentException("Unknown state " + params.tail.head)
      }

      management.coreManagement.setTaskMode(params.head, state)
      "Resumed"
    }
  }

  def name = List("set", "task", "mode")
}

class RescheduleTaskCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {

    if (params.size < 2) {
      wrongParameters("reschedule task <id> <crontab schedule>")
    } else {
      val builder = new StringBuilder
      for (item <- params.tail) {
        builder.append(item).append(" ")
      }
      val crontabSchedule = builder.toString().replaceAll("\"", "").trim

      management.coreManagement.rescheduleTask(params(0), crontabSchedule)
      "Rescheduled"
    }
  }

  def name = List("reschedule", "task")
}

class RemoveScheduledTaskCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {

    params.headOption match {
      case Some(value) => {
        management.coreManagement.removeScheduledTask(value)
        "Removed"
      }
      case None => wrongParameters("remove scheduled task <id>")
    }
  }

  def name = List("remove", "scheduled", "task")
}
