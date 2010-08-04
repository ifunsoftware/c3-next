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

object UserCommands extends Commands {
  def instances = List(
    new ListUsersCommand,
    new AddUserCommand,
    new UpdateUserCommand,
    new DeleteUserCommand
    )
}

class ListUsersCommand extends Command {
  def execute: String = {

    val users = management.listUsers

    val builder = new StringBuilder

    builder.append(String.format("%-12s %-10s %-8s\n", "User name", "Type", "Enabled"))

    for(user <- users){

      builder.append(String.format("%-12s %-10s %-8b\n", user.name, user.role, user.enabled))

    }

    builder.toString
  }

  def name = List("list", "users")

}

class AddUserCommand extends Command {
  def execute = {

    if (params.size < 3) {
      "Not enought params.\nUsage: add user <name> <password> <type>"
    } else {

      val array = params.toArray

      management.addUser(array(0), array(1), array(2))
      "User created"
    }
  }

  def name = List("add", "user")
}

class UpdateUserCommand extends Command {
  def execute = {
    if (params.size < 3) {
      "Not enought params.\nUsage: update user <name> <password> <type> <enabled>"
    } else {

      val array = params.toArray

      management.updateUser(array(0), array(1), array(2), (array(3) == "true"))
      "User updated"
    }

  }

  def name = List("update", "user")

}

class DeleteUserCommand extends Command {
  def execute = {
    if (params.size < 1) {
      "Not enought params.\nUsage: delete user <name>"
    } else {
      management.deleteUser(params.first)
      "User deleted"
    }
  }

  def name = List("delete", "user")

}