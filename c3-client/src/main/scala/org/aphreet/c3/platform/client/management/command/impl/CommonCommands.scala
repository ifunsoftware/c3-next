package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.management.cli.command.{Command, Commands}

object CommonCommands extends Commands {

  def instances = List(
    new EmptyCommand,
    new QuitCommand,
    new ExitCommand
  )
}

class EmptyCommand extends Command {

  override
  def execute() = ""

  def name = List("")

}


class QuitCommand extends Command {

  override
  def execute() = {
    println("Bye")
    System.exit(0)
    ""
  }

  def name = List("quit")
}

class ExitCommand extends QuitCommand {

  override def name = List("exit")
}