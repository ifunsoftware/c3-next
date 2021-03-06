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

object ReplicationCommands extends Commands {

  def instances = List(
    new AddReplicationTarget,
    new RemoveReplicationTarget,
    new ListReplicationTargets,
    new ReplayReplicationQueueCommand,
    new CopyDataToTargetCommand,
    new ResetReplicationQueueCommand,
    new DumpReplicationQueueCommand
  )

}

class AddReplicationTarget extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {
    if (params.size < 4) {
      wrongParameters("create replication target <hostname> <port> <username> <password>")
    } else {

      val paramsArray = params.toArray

      management.replicationManagement.establishReplication(paramsArray(0), paramsArray(1).toInt, paramsArray(2), paramsArray(3))
      "Done"
    }
  }

  def name: List[String] = List("create", "replication", "target")
}

class RemoveReplicationTarget extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {
    if (params.size < 1) {
      "Not enough params.\nUsage: remove replication target <systemid>"
    } else {
      management.replicationManagement.cancelReplication(params.head)
      "Done"
    }
  }

  def name: List[String] = List("remove", "replication", "target")
}

class ListReplicationTargets extends Command {

  override
  def execute(management: PlatformManagementService): String = {
    val targets = management.replicationManagement.listReplicationTargets

    val header = "|        ID       |              Host              |\n" +
      "|-----------------|--------------------------------|\n"

    val footer = "|-----------------|--------------------------------|\n"

    targets.map(e => String.format("| %-15s | %-30s |\n", e.systemId, e.hostname)).foldLeft(header)(_ + _) + footer

  }

  def name: List[String] = List("list", "replication", "targets")
}

class ReplayReplicationQueueCommand extends Command {

  override
  def execute(management: PlatformManagementService): String = {
    management.replicationManagement.replayReplicationQueue()
    "Retry started"
  }

  def name: List[String] = List("start", "replication", "retry")

}

class ResetReplicationQueueCommand extends Command {

  override
  def execute(management: PlatformManagementService): String = {
    management.replicationManagement.resetReplicationQueue()
    "Queue has been reset"
  }

  def name: List[String] = List("reset", "replication", "queue")

}

class DumpReplicationQueueCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {

    params.headOption match {
      case Some(path) => {
        management.replicationManagement.dumpReplicationQueue(path)
        "Task has been submitted"
      }

      case None => wrongParameters("dump replication queue <path>")
    }
  }

  def name: List[String] = List("dump", "replication", "queue")

}

class CopyDataToTargetCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {
    params.headOption match {
      case Some(id) => management.replicationManagement.copyToTarget(id); "Copy task submitted"
      case None => wrongParameters("copy data to target <systemid>")
    }
  }

  def name: List[String] = List("copy", "data", "to", "target")
}
