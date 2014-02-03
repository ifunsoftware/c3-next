package org.aphreet.c3.platform.management.cli.command.impl

import org.aphreet.c3.platform.management.cli.command.{Command, Commands}
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

object FilesystemCommands extends Commands {

  def instances = List(
    new StartFilesystemCheck
  )
}

class StartFilesystemCheck extends Command {

  override
  def execute(management: PlatformManagementService): String = {
    management.fsManagement.startFilesystemCheck()

    "Check started"
  }

  def name = List("start", "filesystem", "check")
}