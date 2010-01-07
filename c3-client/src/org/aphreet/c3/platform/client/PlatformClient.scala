package org.aphreet.c3.platform.client

import java.io.{BufferedReader, InputStreamReader}

import org.springframework.remoting.rmi.RmiProxyFactoryBean

import org.aphreet.c3.platform.management.rmi.PlatformRmiManagementService
import org.aphreet.c3.platform.access.rmi.PlatformRmiAccessService

import command.CommandFactory

object PlatformClient {

  var managementService:PlatformRmiManagementService = null
  
  var accessService:PlatformRmiAccessService = null
  
  var commandFactory:CommandFactory = null
  
  {
    val rmiBean = new RmiProxyFactoryBean
    rmiBean.setServiceUrl("rmi://127.0.0.1:1299/PlatformRmiManagementEndPoint")
    rmiBean.setServiceInterface(classOf[PlatformRmiManagementService])
    rmiBean.afterPropertiesSet
    
    managementService = rmiBean.getObject.asInstanceOf[PlatformRmiManagementService]
    
    val rmiAccess = new RmiProxyFactoryBean
    rmiAccess.setServiceUrl("rmi://127.0.0.1:1299/PlatformRmiAccessEndPoint")
    rmiAccess.setServiceInterface(classOf[PlatformRmiAccessService])
    rmiAccess.afterPropertiesSet
    
    accessService = rmiAccess.getObject.asInstanceOf[PlatformRmiAccessService]
    
    commandFactory = new CommandFactory(accessService, managementService)
  }
  
  
  def main(args : Array[String]) : Unit = {
    
    var shouldExit = false
    
    val reader = new BufferedReader(new InputStreamReader(System.in))
    
    println("Welcome to C3 shell")
    
    print("C3>")
    
    while(!shouldExit){
      val line = reader.readLine
      
      line.toLowerCase.trim match {
        case "quit" => shouldExit = true
        
        case "" => print("C3>")
        
        case _ => {
          commandFactory.getCommand(line) match {
            case Some(command) => {
        	  try{
        	    println(command.execute)
        	  }catch{
        	    case e:Exception=> println("Failed to execute command: " + e.getMessage)
        	    case e => e.printStackTrace
        	  }
          	}
          	case None => println("Command not found")
          }
          print("C3>")
        }
      }
    }
    println("Bye")
  }
}
