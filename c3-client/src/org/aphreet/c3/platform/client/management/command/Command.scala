package org.aphreet.c3.platform.client.management.command

import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService

abstract class Command{

  var params:List[String] = List()
  
  var access:PlatformAccessService = null
  
  var management:PlatformManagementService = null
  
  def execute():String
  
  def name:List[String]
}
