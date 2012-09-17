package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.Command
import collection.immutable.TreeSet

class HelpCommand extends Command{

  override
  def execute():String = {

    HelpCommand.commands.reduceLeft(_ + "\n" + _)
    
  }
  
  def name:List[String] = List("help")
}

object HelpCommand{

  var commands:TreeSet[String] = new TreeSet

  def addCommand(command:Command) {

    val commandLine:String = command.name.foldRight("")(_ + " " + _)

    if(!commandLine.isEmpty){
      commands = commands + commandLine  
    }

  }

}
