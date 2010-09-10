package org.aphreet.c3.platform.client.management

import command.CommandFactory
import connection.impl.{WSConnectionProvider, RmiConnectionProvider}
import connection.{ConnectionException, ConnectionProvider}
import java.io.{InputStreamReader, BufferedReader}
import org.springframework.remoting.{RemoteLookupFailureException, RemoteConnectFailureException}
import org.aphreet.c3.platform.client.common.ArgumentType._
import java.util.logging.LogManager
import org.aphreet.c3.platform.client.common.{VersionUtils, CLI}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 1:48:53 AM
 * To change this template use File | Settings | File Templates.
 */

class ManagementClient(override val args:Array[String]) extends CLI(args) {

  var connectionProvider:ConnectionProvider = null

  val clientName = "Shell"

  {
    LogManager.getLogManager.readConfiguration(getClass.getClassLoader.getResourceAsStream("log.properties"))


    if(cli.getOptions.length == 0) helpAndExit(clientName)

    if(cli.hasOption("help")) helpAndExit(clientName)

    if(cli.hasOption("ignoreSSLHostname")) disableHostNameVerification

    val connectionType = cliValue("t", "rmi").toLowerCase

    connectionProvider = connectionType match {
      case "rmi" => new RmiConnectionProvider
      case "ws" => {
        val host = cliValue("h", "http://localhost:9301")
        val user = cliValue("u", "")
        val password = cliValue("p", "")
        try{
          new WSConnectionProvider(host, user, password)
        }catch{
          case e:ConnectionException => {
            println("Failed to connect to server. Error message is: " + e.getMessage)
            System.exit(1)
            null
          }
        }
      }
      case _ => throw new IllegalArgumentException("Unknown connection type")

    }
  }


  def cliDescription = parameters(
    "t" has mandatory argument "type" described "Connection type WS|RMI. Default is RMI",
    "h" has mandatory argument "hostname" described "Host to connect to. Only for WS. Default is http://localhost:9301",
    "u" has mandatory argument "username" described "Only for WS",
    "p" has mandatory argument "password" described "Only fir WS",
    "ignoreSSLHostname" described  "Ignore host name verification error",
    "help" described "Prints this message"
  )




  def run = {

    var shouldExit = false

    val reader = new BufferedReader(new InputStreamReader(System.in))

    println("C3 Client version " + VersionUtils.clientVersion)

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
        }case e:ConnectionException => {
          println("Failed to connect to server. Error message is: " + e.getMessage)
        }
      }
    }
    null
  }

  def disableHostNameVerification = {
    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
      new javax.net.ssl.HostnameVerifier() {
        override def verify(hostname: String, sslSession: javax.net.ssl.SSLSession): Boolean = true
      }
    )
  }
}