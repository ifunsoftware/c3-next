package org.aphreet.c3.platform.remote.servlet

import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

trait CLIEvaluator {

  def evaluate(command:String, access:PlatformAccessService, management:PlatformManagementService):String

}