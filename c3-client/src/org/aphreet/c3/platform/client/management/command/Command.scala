package org.aphreet.c3.platform.client.management.command

import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import java.io.{InputStreamReader, BufferedReader}

abstract class Command{

  val reader = new BufferedReader(new InputStreamReader(System.in))

  var params:List[String] = List()
  
  var access:PlatformAccessService = null
  
  var management:PlatformManagementService = null
  
  def execute():String
  
  def name:List[String]

  def readInput:String = reader.readLine

  def writeString(line:String) = print(line)

}

abstract class Commands{

  def instances:List[Command]
}