package org.aphreet.c3.platform.remote.impl

import org.aphreet.c3.platform.remote.servlet.CLIEvaluator
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.management.cli.command.CommandFactory
import scala.Some
import org.springframework.stereotype.Component
import org.apache.commons.logging.LogFactory

@Component
class CLICommandEvaluatorImpl extends CLIEvaluator {

  val log = LogFactory.getLog(getClass)

  val commandFactory = new CommandFactory()

  {
    log.info("Creating CLI evaluator")
  }

  def evaluate(command: String, access: PlatformAccessService, management: PlatformManagementService) = {
    commandFactory.getCommand(command) match {
      case Some(execution) => execution.command.execute(execution.params, access, management)
      case None => "Unknown command"
    }
  }
}