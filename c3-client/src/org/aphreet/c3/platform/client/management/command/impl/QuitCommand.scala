package org.aphreet.c3.platform.client.management.command.impl

class QuitCommand extends Command{

  override def execute:String = {
    println("Bye")
    System.exit(0)
    ""
  }
  
  override def name:List[String] = List("quit")
}
