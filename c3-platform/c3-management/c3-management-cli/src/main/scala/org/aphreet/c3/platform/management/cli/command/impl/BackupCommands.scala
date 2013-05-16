package org.aphreet.c3.platform.management.cli.command.impl

import org.aphreet.c3.platform.management.cli.command.{Commands, Command}
import org.aphreet.c3.platform.remote.api.management.{TargetDescription, PlatformManagementService}
import org.apache.commons.codec.binary.Base64

object BackupCommands extends Commands{
  def instances = List(new RestoreBackupCommand, new CreateBackupCommand,
    new ListBackupsCommand, new CreateLocalTargetCommand, new CreateRemoteTargetCommand,
    new RemoveTargetCommand, new ListTargetsCommand, new ShowTargetInfoCommand, new ScheduleBackupCommand)
}


class RestoreBackupCommand extends Command{

  override
  def execute(params: List[String], management:PlatformManagementService):String = {
    if (params.size < 2) {
      wrongParameters("restore backup <target id / number> <name>")
    } else {
      management.restoreBackup(params(0), params(1))
      ""
    }
  }

  def name = List("restore" , "backup")
}


class CreateBackupCommand extends Command{

  override
  def execute(params: List[String], management:PlatformManagementService):String = {

    params.headOption match {
      case Some(value) => {
        management.createBackup(value)
        ""
      }
      case None => wrongParameters("create backup <target id / number>")
    }
  }

  def name = List("create", "backup")
}

class ScheduleBackupCommand extends Command {

  override
  def execute(params: List[String], management:PlatformManagementService):String = {

    if (params.size < 2) {
      wrongParameters("schedule backup <target id / number> <crontab schedule>")
    } else {
      val builder = new StringBuilder
      for (item <- params.tail) {
        builder.append(item).append(" ")
      }
      val crontabSchedule = builder.toString().replaceAll("\"", "").trim

      management.scheduleBackup(params(0), crontabSchedule)
      "Backup schedule was created"
    }
  }

  def name = List("schedule", "backup")
}


class ListBackupsCommand extends Command {

  val headerTemplate = "Backups in target %s:"

  override def execute(params: List[String], management:PlatformManagementService):String = {

    params.headOption match {
      case Some(value) => management.listBackups(value).foldLeft(String.format(headerTemplate, value))(_ + "\n" +  _)

      case None => wrongParameters("list backups <target id / number>")
    }
  }

  def name = List("list", "backups")
}


class CreateLocalTargetCommand extends Command {

  override def execute(params: List[String], management: PlatformManagementService):String = {

    if (params.size < 2) {
      wrongParameters("create target local <id> <path>")
    } else {
      management.createLocalBackupTarget(params(0), params(1))
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

      val encodedKey = paramsArray(4)

      val key = new String(Base64.decodeBase64(encodedKey), "UTF-8")

      management.createRemoteBackupTarget(paramsArray(0), paramsArray(1), paramsArray(2), paramsArray(3), key)
      "Remote target created"
    }
  }

  def name = List("create", "target", "remote")
}

class RemoveTargetCommand extends Command {

  override def execute(params: List[String], management: PlatformManagementService):String = {

    params.headOption match {
      case Some(id) => {
        management.removeBackupTarget(id)
        "Target deleted"
      }

      case None => wrongParameters("remove target <id / number>")
    }
  }

  def name = List("remove", "target")
}


class ListTargetsCommand extends Command {

  val header = "| No |       ID       |               Host               |                   Folder                   |\n" +
               "|----|----------------|----------------------------------|--------------------------------------------|\n"
  val footer = "|----|----------------|----------------------------------|--------------------------------------------|\n"

  def format(counter: Int, desc: TargetDescription):String =
        String.format("| %2s | %14s | %-32s | %42s |\n",
          counter.toString,
          desc.id,
          desc.backupType match {
            case "local" => "localhost"
            case "remote" => desc.host
            case _ => ""
          },
          desc.folder)

  override def execute(management: PlatformManagementService):String = {
    val builder = new StringBuilder(header)
    var counter = 1

    for (target <- management.listBackupTargets()) {
      builder.append(format(counter, target))
      counter += 1
    }
    builder.append(footer)

    builder.toString()
  }

  def name = List("list", "targets")
}

class ShowTargetInfoCommand extends Command {

  override def execute(params: List[String], management: PlatformManagementService):String = {

    params.headOption match {
      case Some(value) => {
        val desc = management.showBackupTargetInfo(value)

        val builder = new StringBuilder("Target info\n")
        builder.append("ID: ").append(desc.id).append("\n")

        val isRemote = desc.backupType.equals("remote")
        builder.append("Type: ").append(desc.backupType).append("\n")

        if (isRemote) {
          builder.append("Host: ").append(desc.host).append("\n")
          builder.append("User: ").append(desc.user).append("\n")
        }

        builder.append("Folder: ").append(desc.folder).append("\n")

        if (isRemote) {
          builder.append("Key: ").append(desc.privateKey).append("\n")
        }

        builder.toString()
      }

      case None => wrongParameters("show target <target id / target number>")
    }
  }

  def name = List("show", "target")
}
