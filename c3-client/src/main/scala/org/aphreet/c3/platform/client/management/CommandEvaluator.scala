package org.aphreet.c3.platform.client.management

import command.{InteractiveCommand, CommandExecution}
import jline.ConsoleReader
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

class CommandEvaluator(val reader:ConsoleReader, val accessService:PlatformAccessService, val managementService:PlatformManagementService) {

  def evaluate(execution:CommandExecution):String = {
    if (classOf[InteractiveCommand].isAssignableFrom(execution.command.getClass)){
      execution.command.asInstanceOf[InteractiveCommand].execute(execution.params, managementService, reader)
    }else{
      execution.command.execute(execution.params, accessService, managementService)
    }
  }
}
