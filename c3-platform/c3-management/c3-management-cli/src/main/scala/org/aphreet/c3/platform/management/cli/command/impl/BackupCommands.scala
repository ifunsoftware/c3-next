package org.aphreet.c3.platform.management.cli.command.impl

import org.aphreet.c3.platform.management.cli.command.{Commands, Command}
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

object BackupCommands extends Commands{
  def instances = List(new RestoreBackupCommand, new CreateBackupCommand,
    new ListBackupsCommand, new CreateLocalTargetCommand, new CreateRemoteTargetCommand,
    new RemoveTargetCommand, new ListTargetsCommand, new ShowTargetInfoCommand)
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

  def name = List("list", "backups")
}


class CreateLocalTargetCommand extends Command {

  override def execute(params: List[String], management: PlatformManagementService):String = {

    if (params.size < 2) {
      wrongParameters("create target local <id> <path>")
    } else {
      management.createLocalTarget(params(0), params(1))
      "Local target created"
    }
  }

  def name = List("create", "target", "local")
}


class CreateRemoteTargetCommand extends Command {

  override def execute(params: List[String], management: PlatformManagementService):String = {

    if (params.size < 5) {
      wrongParameters("create target remote <id> <host> <user> <path> <private key file path>")
    } else {
      val paramsArray = params.toArray
      management.createRemoteTarget(paramsArray(0), paramsArray(1), paramsArray(2), paramsArray(3), paramsArray(4))
      "Remote target created"
    }
  }

  def name = List("create", "target", "remote")
}

class RemoveTargetCommand extends Command {

  override def execute(params: List[String], management: PlatformManagementService):String = {

    params.headOption match {
      case Some(id) => {
        management.removeTarget(id)
        "Target deleted"
      }

      case None => wrongParameters("remove target <id / number>")
    }
  }

  def name = List("remove", "target")
}


class ListTargetsCommand extends Command {

  override def execute(management: PlatformManagementService):String = {

    val targets = management.listTargets()

    val builder = new StringBuilder
    for (target <- targets) {
      builder.append(target).append("\n")
    }

    builder.toString()
  }

  def name = List("list", "targets")
}

class ShowTargetInfoCommand extends Command {
  override def execute(params: List[String], management: PlatformManagementService):String = {

    params.headOption match {
      case Some(value) => management.showTargetInfo(value)

      case None => wrongParameters("show target <target id / target number>")
    }
  }

  def name = List("show", "target")
}
