package org.aphreet.c3.platform.client

import java.io.{BufferedReader, InputStreamReader}

import org.springframework.remoting.rmi.RmiProxyFactoryBean
import org.springframework.remoting.{RemoteConnectFailureException, RemoteLookupFailureException}


import org.aphreet.c3.platform.management.rmi.PlatformRmiManagementService
import org.aphreet.c3.platform.access.rmi.PlatformRmiAccessService

import command.CommandFactory

object PlatformClient {

  def main(args : Array[String]) : Unit = {
    
    var shouldExit = false
    
    val reader = new BufferedReader(new InputStreamReader(System.in))
    
    var commandFactory = connect
    
    println("Welcome to C3 shell")
    
    print("C3>")
    
    
    while(true){
      val line = reader.readLine.toLowerCase.trim
      
      var success = false
      
      for(i <- 1 to 5 if !success){
        success = commandFactory.getCommand(line) match {
          case Some(command) => {
        	  try{
        	    println(command.execute)
        	    true
        	  }catch{
        	    case e:RemoteConnectFailureException=> {
        	      println("Connection to server lost. Trying to reconnect...")
        	      
        	      commandFactory = connect
        	      false
        	    }
        	    case e =>{
        	      println("Failed to execute command: " + e.getMessage)
        	      true
                }
        	  }
          	}
           case None => {
             println("Command not found")
             true
           }
        }
      }
      
      if(!success){
        println("Failed to establish connection to server")
        System.exit(0)
      }
      print("C3>")
    }
  }
  
  def createCommandFactory:CommandFactory = {
    val rmiBean = new RmiProxyFactoryBean
    rmiBean.setServiceUrl("rmi://127.0.0.1:1299/PlatformRmiManagementEndPoint")
    rmiBean.setServiceInterface(classOf[PlatformRmiManagementService])
    rmiBean.afterPropertiesSet
    
    val managementService = rmiBean.getObject.asInstanceOf[PlatformRmiManagementService]
    
    val rmiAccess = new RmiProxyFactoryBean
    rmiAccess.setServiceUrl("rmi://127.0.0.1:1299/PlatformRmiAccessEndPoint")
    rmiAccess.setServiceInterface(classOf[PlatformRmiAccessService])
    rmiAccess.afterPropertiesSet
    
    val accessService = rmiAccess.getObject.asInstanceOf[PlatformRmiAccessService]
    
    new CommandFactory(accessService, managementService)
  }
  
  def connect:CommandFactory = {
    for(i <- 1 to 5){
      try{
        return createCommandFactory
      }catch{
        case e:RemoteLookupFailureException => {
          if(i < 5){
            println("Failed to connect to server. Trying to reconnect...")
            Thread.sleep(i * 2000)
          }else{
            println("Can't connect to server")
            System.exit(0)
          }
        }
      }
    }
    null
  }
}
