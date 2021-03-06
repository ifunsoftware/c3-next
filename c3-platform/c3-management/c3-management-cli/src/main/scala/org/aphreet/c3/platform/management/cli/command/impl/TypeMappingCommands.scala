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

import org.aphreet.c3.platform.management.cli.command.{Commands, Command}
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

object TypeMappingCommands extends Commands {

  def instances = List(
    new AddTypeMappingCommand,
    new DeleteTypeMappingCommand,
    new ListTypeMappingCommand
  )
}

class AddTypeMappingCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {
    if (params.size < 2)
      wrongParameters("create type mapping <mimetype> <versioned>")
    else {

      val mimeType = params.head
      val versioned = params(1) == "true"

      management.coreManagement.addTypeMapping(mimeType, versioned)

      "Type mapping added"
    }
  }

  def name = List("create", "type", "mapping")

}

class DeleteTypeMappingCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {

    if (params.size < 1) {
      wrongParameters("remove type mapping <mimetype>")
    } else {
      management.coreManagement.removeTypeMapping(params.head)
      "Type mapping removed"
    }
  }

  def name = List("remove", "type", "mapping")
}

class ListTypeMappingCommand extends Command {

  override
  def execute(management: PlatformManagementService): String = {

    val builder = new StringBuilder

    for (mapping <- management.coreManagement.listTypeMappings)
      builder.append(String.format("%20s %s\n", mapping._1, mapping._2.toString))


    builder.toString()

  }

  def name = List("list", "type", "mappings")


}