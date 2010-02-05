package org.aphreet.c3.platform.client.management.command.impl

class HelpCommand extends Command{

  def execute():String = {
    
    HelpCommand.commandList
      .map(c => c.name.foldRight("")(_ + " " + _)).foldRight("")(_ + "\n" + _)
    
  }
  
  def name:List[String] = List("help")
}

object HelpCommand{
  
  var commandList:List[Command] = List()
  
}
