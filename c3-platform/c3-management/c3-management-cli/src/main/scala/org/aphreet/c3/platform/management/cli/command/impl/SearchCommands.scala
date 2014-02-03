package org.aphreet.c3.platform.management.cli.command.impl

import org.aphreet.c3.platform.management.cli.command.{Command, Commands}
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

object SearchCommands extends Commands {

  def instances = List(new ResetSearchIndexCommand, new DumpSearchIndexCommand, new DeleteSearchIndexCommand)

}

class ResetSearchIndexCommand extends Command {

  override def execute(management: PlatformManagementService): String = {
    management.coreManagement.setPlatformProperty("c3.search.index.create_timestamp", System.currentTimeMillis().toString)
    "Search index will be recreated"
  }

  def name = List("reset", "search", "index")
}

class DeleteSearchIndexCommand extends Command {

  override def execute(management: PlatformManagementService): String = {
    management.searchManagement.deleteIndexes()
    "Search index dropped and will be recreated"
  }

  def name = List("drop", "search", "index")
}

class DumpSearchIndexCommand extends Command {

  override def execute(params: List[String], management: PlatformManagementService): String = {

    params.headOption match {
      case Some(path) => {
        management.searchManagement.dumpIndex(path)
        "Search index is being dumped to path " + path
      }
      case None => wrongParameters("dump search index <path>")
    }
  }

  def name = List("dump", "search", "index")


}