package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.{Command, Commands}

object FilesystemCommands extends Commands{

  def instances = List(
    new StartFilesystemCheck
  )
}

class StartFilesystemCheck extends Command{

  def execute:String = {
    management.startFilesystemCheck

    "Check started"
  }

  def name = List("start", "filesystem", "check")
}