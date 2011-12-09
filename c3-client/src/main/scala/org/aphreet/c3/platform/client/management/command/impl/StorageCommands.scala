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
import org.aphreet.c3.platform.remote.api.management.{StorageIndexDescription, StorageDescription}

object StorageCommands extends Commands{

  def instances = List(
    new AddStorageCommand,
    new DeleteStorageCommand,
    new SetStorageModeCommand,
    new ListStorageCommand,
    new ListStorageTypesCommand,
    new ShowResourceCommand,
    new ShowStorageCommand,
    new CreateStorageIndexCommand,
    new RemoveStorageIndexCommand
    )
}

class AddStorageCommand extends Command{

  def execute():String = {
    if(params.size < 2){
      wrongParameters("create storage <type> <path>")
    }else{
      management.createStorage(params.head, params.tail.head)
      "Storage created"
    }
  }

  def name = List("create", "storage")
}



class DeleteStorageCommand extends Command{

  def execute():String = {
    if(params.size < 1){
      wrongParameters("remove storage <storage id>")
    }else{
      management.removeStorage(params.head)
      "Storage deleted"
    }
  }

  def name = List("remove", "storage")

}

class SetStorageModeCommand extends Command{

  def execute():String = {

    if(params.size < 2){
      "Not enought params.\nUsage: set storage mode <id> <mode>"
    }else{
      management.setStorageMode(params.head, params.tail.head)
      "Mode set"
    }
  }

  def name = List("set", "storage", "mode")
}

class ListStorageCommand extends Command {

  def format(desc:StorageDescription):String =
    String.format("| %-20s | %4s | %-10s | %6d | %-30s |\n",
      desc.storageType,
      desc.id,
      desc.mode,
      desc.count,
      desc.path)

  val header = "|         Type         |  ID  |    Mode    | Count  |              Path              |\n" +
          "|----------------------|------|------------|--------|--------------------------------|\n"
  val footer = "|----------------------|------|------------|--------|--------------------------------|\n"

  def execute():String = {

    management.listStorages.map(s => format(s)).foldLeft(header)(_ + _) + footer

  }

  def name = List("list", "storages")
}

class ListStorageTypesCommand extends Command{

  def execute():String = {
    val types = management.listStorageTypes

    val builder = new StringBuilder

    for(tp <- types){
      builder.append(tp).append("\n")
    }

    builder.toString()

  }

  def name = List("list", "storage", "types")
}

class ShowResourceCommand extends Command {

  def execute() = {

    if(params.length < 1){
      "Not enought params.\nUsage: show resource <address>"
    }else{
      val result = access.getResourceAsString(params.head)
      if(result != null){
        result
      }else{
        "Resource not found"
      }
    }
  }

  def name = List("show", "resource")
}

class ShowStorageCommand extends Command {

  def execute() = {
    if(params.length < 1){
      "Not enough params.\nUsage: show storage <storage id>"
    }else{
      val storages = management.listStorages.filter(_.id == params.head)

      if(storages.size > 0){
        val storage = storages(0)

        val storageIds = if(storage.ids != null){
          storage.ids.foldRight("")(_ + ", " + _)
        }else{
          ""
        }

        val indexes = if(storage.indexes != null){
          storage.indexes.map(idx => {
            idx.name + " (" + idx.fields.reduceRight(_ + ", " + _) + ") sys:" + idx.system + " mul:" + idx.multi + " date:" + idx.created
          }).foldRight("")(_ + "\n" + _)
        }else{
          ""
        }

        "Storage:\n" +
                "Id     : " + storage.id + "\n" +
                "Sec ids: " + storageIds + "\n" +
                "Type   : " + storage.storageType + "\n" +
                "Path   : " + storage.path + "\n" +
                "Mode   : " + storage.mode + "\n" +
                "Res.cnt: " + storage.count + "\n" +
                "Indexes:\n" + indexes

      }else{
        "Storage with id \"" + params.head + "\" is not found"
      }
    }
  }

  def name = List("show", "storage")
}

class CreateStorageIndexCommand extends Command {

  def execute() = {
    if(params.length < 5){
      "Not enough arguments.\nUsage: create storage index <storage id> <index name> <system?> <multi?> <field0> <field1> ..."
    }else{
      val indexParams = params.toArray

      val id = indexParams(0)
      val name = indexParams(1)
      val system = indexParams(2) == "true"
      val multi = indexParams(3) == "true"
      val fields = params.drop(4).toArray


      management.createIndex(id, name, fields, system, multi)

      "Index created"
    }
  }

  def name = List("create", "storage", "index")

}

class RemoveStorageIndexCommand extends Command {

  def execute() = {
    if(params.length < 2){
      "Not enough arguments.\nUsage: remove storage index <storage id> <index name>"
    }else{
      management.removeIndex(params.head, params.tail.head)
      "Index removed"
    }
  }

  def name = List("remove", "storage", "index")
}
