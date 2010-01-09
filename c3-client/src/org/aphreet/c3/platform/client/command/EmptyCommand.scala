package org.aphreet.c3.platform.client.command

class EmptyCommand extends Command{

  override def execute:String = ""
  
  override def name:List[String] = List("")
  
}
