package org.aphreet.c3.platform.management.cli.command.impl

import org.aphreet.c3.platform.management.cli.command.{Commands, Command}
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

object BackupCommands extends Commands{
  def instances = List(new RestoreBackupCommand,
    new CreateBackupCommand, new ListBackupsCommand)
}


class RestoreBackupCommand extends Command{

  override
  def execute(params: List[String], management:PlatformManagementService):String = {
    params.headOption match {
      case Some(value) => management.restoreBackup(value); ""
      case None => wrongParameters("restore backup <path>")
    }
  }

  def name = List("restore" , "backup")
}


class CreateBackupCommand extends Command{

  override
  def execute(management:PlatformManagementService):String = {
    management.createBackup()
    ""
  }

  def name = List("create", "backup")
}


class ListBackupsCommand extends Command {

  override def execute(params: List[String], management:PlatformManagementService):String = {

    params.headOption match {
      case Some(value) => {
        val backups = management.listBackups(value)

        val builder = new StringBuilder
        for (b <- backups) {
          builder.append(b).append("\n")
        }

        builder.toString()
      }

      case None => wrongParameters("list backups <path>")
    }
  }

  def name = List("list" , "backups")
}
