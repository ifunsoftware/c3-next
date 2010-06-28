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

import org.aphreet.c3.platform.client.management.command._

object SizeMappingCommands extends Commands{

  def instances = List(
    new AddSizeMappingCommand,
    new DeleteSizeMappingCommand,
    new ListSizeMappingCommand
  )

}

class AddSizeMappingCommand extends Command {

  def execute = {
    if (params.size < 3)
      "Not enough params.\nUsage: add size mapping <size> <storagetype> <versioned>"
    else {

      val size = params.first.toLong
      val storageType = params.tail.first
      val versioned = if (params(2) == "true") 1 else 0


      management.addSizeMapping(size, storageType, versioned)

      "Size mapping added"
    }
  }

  def name = List("add", "size", "mapping")
}

class DeleteSizeMappingCommand extends Command {

  def execute = {

    if (params.size < 1) {
      "Not enough params.\nUsage remove size mapping <size>"
    } else {
      management.removeSizeMapping(params.first.toLong)
      "Size mapping deleted"
    }
  }

  def name = List("delete", "size", "mapping")

}

class ListSizeMappingCommand extends Command {

  def execute = {
    val builder = new StringBuilder

    for (mapping <- management.listSizeMappings)
      builder.append(String.format("%10d %20s %d\n", mapping.size, mapping.storage, mapping.versioned))


    builder.toString
  }

  def name = List("list", "size", "mappings")
}