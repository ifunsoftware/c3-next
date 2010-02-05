package org.aphreet.c3.platform.client.management.command.impl

class ErrorCommand(val message:String) extends Command{

  def execute:String = message
  
  def name = List()
}
