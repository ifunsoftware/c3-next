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

import org.aphreet.c3.platform.client.management.command.{Commands, Command}

object TypeMappingCommands extends Commands{

  def instances = List(
      new AddTypeMappingCommand,
      new DeleteTypeMappingCommand,
      new ListTypeMappingCommand
  )
}

class AddTypeMappingCommand extends Command{

  def execute:String = {
    if(params.size < 3)
      "Not enough params.\nUsage: add type mapping <mimetype> <storagetype> <versioned>"
    else{

      val mimeType = params.head
      val storageType = params.tail.head
      val versioned = (params(2) == "true")

      management.addTypeMapping(mimeType, storageType, versioned)

      "Type mapping added"
    }
  }

  def name = List("add", "type", "mapping")

}

class DeleteTypeMappingCommand extends Command{

  def execute:String = {

    if(params.size < 1){
      "Not enough params.\nUsage delete type mapping <mimetype>"
    }else{
      management.removeTypeMapping(params.head)
      "Type mapping deleted"
    }
  }

  def name = List("delete", "type", "mapping")
}

class ListTypeMappingCommand extends Command{

  def execute:String = {

    val builder = new StringBuilder

    for(mapping <- management.listTypeMappings)
      builder.append(String.format("%20s %20s %b\n", mapping.mimeType, mapping.storage, mapping.versioned))


    builder.toString

  }

  def name = List("list", "type", "mappings")


}