package org.aphreet.c3.platform.client.command

class QuitCommand extends Command{

  override def execute:String = {
    println("Bye")
    System.exit(0)
    ""
  }
  
  override def name:List[String] = List("quit")
}
