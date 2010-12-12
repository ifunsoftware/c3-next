/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.platform.client.management

import command.CommandFactory
import connection.impl.{WSConnectionProvider, RmiConnectionProvider}
import connection.{ConnectionException, ConnectionProvider}
import java.io.{InputStreamReader, BufferedReader}
import org.springframework.remoting.{RemoteLookupFailureException, RemoteConnectFailureException}
import org.aphreet.c3.platform.client.common.ArgumentType._
import java.util.logging.LogManager
import org.aphreet.c3.platform.client.common.{VersionUtils, CLI}

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