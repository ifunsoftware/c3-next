package org.aphreet.c3.platform.client.management

import java.io.{BufferedReader, InputStreamReader}

import command._
import org.springframework.remoting.RemoteConnectFailureException
import org.springframework.remoting.RemoteLookupFailureException

import org.aphreet.c3.platform.remote.rmi.access.PlatformRmiAccessService
import org.aphreet.c3.platform.remote.rmi.management.PlatformRmiManagementService

import org.aphreet.c3.platform.client.common.SpringRmiAccessor

object PlatformManagementClient extends SpringRmiAccessor{

  def main(args : Array[String]) : Unit = {
    
    var shouldExit = false
    
    val reader = new BufferedReader(new InputStreamReader(System.in))
    
    var commandFactory = connect
    
    println("Welcome to C3 shell")
    
    print("C3>")
    
    
    while(true){
      val line = reader.readLine.trim
      
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
        	      println("Failed to execute command: " + e.getClass.getSimpleName + " " + e.getMessage)
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
    
    val management = obtainRmiService("rmi://127.0.0.1:1299/PlatformRmiManagementEndPoint", 
                                      classOf[PlatformRmiManagementService])
    
    val access = obtainRmiService("rmi://127.0.0.1:1299/PlatformRmiAccessEndPoint", 
                                  classOf[PlatformRmiAccessService])
    
    new CommandFactory(access, management)
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
