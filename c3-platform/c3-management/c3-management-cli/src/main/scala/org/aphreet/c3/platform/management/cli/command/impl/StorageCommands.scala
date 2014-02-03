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

import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.management.cli.command.{Commands, Command}
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.storage._

object StorageCommands extends Commands {

  def instances = List(
    new AddStorageCommand,
    new DeleteStorageCommand,
    new SetStorageModeCommand,
    new ListStorageCommand,
    new ListStorageTypesCommand,
    new ShowResourceCommand,
    new ShowStorageCommand,
    new CreateStorageIndexCommand,
    new RemoveStorageIndexCommand,
    new StorageSummaryCommand,
    new PurgeStorageData
  )
}

class AddStorageCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {
    if (params.size < 1) {
      wrongParameters("create storage <type> [<path>]")
    } else {
      management.coreManagement.createStorage(params.head, params.tail.headOption.getOrElse(""))
      "Storage created"
    }
  }

  def name = List("create", "storage")
}


class DeleteStorageCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {
    if (params.size < 1) {
      wrongParameters("remove storage <storage id>")
    } else {
      management.coreManagement.removeStorage(params.head)
      "Storage deleted"
    }
  }

  def name = List("remove", "storage")

}

class SetStorageModeCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService): String = {

    if (params.size < 2) {
      "Not enought params.\nUsage: set storage mode <id> RW|RO|U"
    } else {

      val storageId = params.head

      val storageMode = params.tail.head match {
        case "RW" => RW(STORAGE_MODE_USER)
        case "RO" => RO(STORAGE_MODE_USER)
        case "U" => U(STORAGE_MODE_USER)
        case _ => throw new StorageException("No mode named " + params.tail.head)
      }

      management.coreManagement.setStorageMode(storageId, storageMode)
      "Mode set"
    }
  }

  def name = List("set", "storage", "mode")
}

class ListStorageCommand extends Command {

  def format(desc: Storage): String =
    String.format("| %-20s | %4s | %-10s | %6d | %-30s |\n",
      Array(desc.params.storageType,
        desc.id,
        desc.mode.toString,
        desc.count,
        desc.path.stringValue))

  val header = "|         Type         |  ID  |    Mode    | Count  |              Path              |\n" +
    "|----------------------|------|------------|--------|--------------------------------|\n"
  val footer = "|----------------------|------|------------|--------|--------------------------------|\n"

  override
  def execute(management: PlatformManagementService): String = {
    management.coreManagement.listStorages.map(format).foldLeft(header)(_ + _) + footer
  }

  def name = List("list", "storages")
}

class ListStorageTypesCommand extends Command {

  override
  def execute(management: PlatformManagementService): String = {
    val types = management.coreManagement.listStorageTypes

    val builder = new StringBuilder

    for (tp <- types) {
      builder.append(tp).append("\n")
    }

    builder.toString()

  }

  def name = List("list", "storage", "types")
}

class ShowResourceCommand extends Command {

  override
  def execute(params: List[String], access: PlatformAccessService, management: PlatformManagementService) = {

    if (params.length < 1) {
      "Not enought params.\nUsage: show resource <address>"
    } else {
      val result = access.getResourceAsString(params.head)
      if (result != null) {
        result
      } else {
        "Resource not found"
      }
    }
  }

  def name = List("show", "resource")
}

class ShowStorageCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService) = {
    if (params.length < 1) {
      "Not enough params.\nUsage: show storage <storage id>"
    } else {
      val storages = management.coreManagement.listStorages.filter(_.id == params.head)

      if (storages.size > 0) {
        val storage = storages(0)

        val indexes = if (storage.params.indexes != null) {
          storage.params.indexes.map(idx => {
            idx.name + " (" + idx.fields.reduceRight(_ + ", " + _) + ") sys:" + idx.system + " mul:" + idx.multi + " date:" + idx.created
          }).foldRight("")(_ + "\n" + _)
        } else {
          ""
        }

        "Storage:\n" +
          "Id     : " + storage.id + "\n" +
          "Type   : " + storage.params.storageType + "\n" +
          "Path   : " + storage.path + "\n" +
          "Mode   : " + storage.mode + "\n" +
          "Res.cnt: " + storage.count + "\n" +
          "Indexes:\n" + indexes

      } else {
        "Storage with id \"" + params.head + "\" is not found"
      }
    }
  }

  def name = List("show", "storage")
}

class CreateStorageIndexCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService) = {
    if (params.length < 4) {
      "Not enough arguments.\nUsage: create storage index <index name> <system?> <multi?> <field0> <field1> ..."
    } else {
      val indexParams = params.toArray

      val name = indexParams(0)
      val system = indexParams(1) == "true"
      val multi = indexParams(2) == "true"
      val fields = params.drop(3)

      management.coreManagement.createIndex(StorageIndex(name, fields, multi, system, System.currentTimeMillis()))

      "Index created"
    }
  }

  def name = List("create", "storage", "index")

}

class RemoveStorageIndexCommand extends Command {

  override
  def execute(params: List[String], management: PlatformManagementService) = {
    if (params.length < 1) {
      "Not enough arguments.\nUsage: remove storage index <index name>"
    } else {
      management.coreManagement.removeIndex(params.head)
      "Index removed"
    }
  }

  def name = List("remove", "storage", "index")
}

class StorageSummaryCommand extends Command {

  override
  def execute(management: PlatformManagementService) = {

    val resourceNumber = management.coreManagement.listStorages.foldLeft(0l)(_ + _.count.longValue())

    "The system is happily keeping " + resourceNumber + " resources"
  }

  def name = List("show", "system", "summary")
}

class PurgeStorageData extends Command {

  override
  def execute(management: PlatformManagementService): String = {
    management.coreManagement.purgeStorageData()

    "All storage data purged"
  }

  def name = List("purge", "storage", "data")
}
