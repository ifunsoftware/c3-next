package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.Command

class ErrorCommand(val message:String) extends Command{

  def execute:String = message
  
  def name = List()
}
