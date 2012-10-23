package org.aphreet.c3.platform.management.cli.command.impl

import org.aphreet.c3.platform.management.cli.command.{Command, Commands}
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

object SearchCommands extends Commands{

  def instances = List(new ResetSearchIndexCommand)

}

class ResetSearchIndexCommand extends Command{

  override def execute(management:PlatformManagementService):String = {
    management.resetSearchIndex()
    "Search index will be recreated"
  }

  def name = List("reset", "search", "index")
}
