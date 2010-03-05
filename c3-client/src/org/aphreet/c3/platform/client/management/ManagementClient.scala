package org.aphreet.c3.platform.client.management

import command.CommandFactory
import connection.ConnectionProvider
import connection.impl.{WSConnectionProvider, RmiConnectionProvider}
import java.io.{InputStreamReader, BufferedReader}
import org.springframework.remoting.{RemoteLookupFailureException, RemoteConnectFailureException}
import org.aphreet.c3.platform.client.common.CLI
import org.aphreet.c3.platform.client.common.ArgumentType._

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 1:48:53 AM
 * To change this template use File | Settings | File Templates.
 */

class ManagementClient(override val args:Array[String]) extends CLI(args) {

  var connectionProvider:ConnectionProvider = null

  {
    if(cli.getOptions.length == 0) helpAndExit("Shell")

    if(cli.hasOption("help")) helpAndExit("Shell" )

    val connectionType = cliValue("t", "rmi").toLowerCase

    connectionProvider = connectionType match {
      case "rmi" => new RmiConnectionProvider
      case "ws" => {
        val host = cliValue("h", "localhost:8080")
        val user = cliValue("u", "")
        val password = cliValue("p", "")
        new WSConnectionProvider(host, user, password)
      }
      case _ => throw new IllegalArgumentException("Unknown connection type")

    }
  }


  def cliDescription = parameters(
    "t" has mandatory argument "type" described "Connection type WS|RMI. Default is RMI",
    "h" has mandatory argument "hostname" described "Host to connect to. Only for WS. Default is localhost:8080",
    "u" has mandatory argument "username" described "Only for WS",
    "p" has mandatory argument "password" described "Only fir WS",
    "help" described "Prints this message"
  )




  def run = {

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
        	      e.printStackTrace
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
    new CommandFactory(connectionProvider.access, connectionProvider.management)
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