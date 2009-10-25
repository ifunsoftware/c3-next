package org.aphreet.c3.platform.client.command

import org.aphreet.c3.platform.management.rmi.PlatformRmiManagementService
import org.aphreet.c3.platform.access.rmi.PlatformRmiAccessService

abstract class Command{

  var params:List[String] = List()
  
  var access:PlatformRmiAccessService = null
  
  var management:PlatformRmiManagementService = null
  
  def execute():String
  
  def name:List[String]
}
