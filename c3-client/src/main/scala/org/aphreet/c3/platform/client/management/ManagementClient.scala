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

import command.impl.{CommonCommands, PrepareForReplication, SearchCommands}
import connection.impl.{WSConnectionProvider, RmiConnectionProvider}
import connection.{ConnectionException, ConnectionProvider}
import org.springframework.remoting.{RemoteLookupFailureException, RemoteConnectFailureException}
import org.aphreet.c3.platform.client.common.ArgumentType._
import java.util.logging.LogManager
import org.aphreet.c3.platform.client.common.{VersionUtils, CLI}
import java.io.File
import jline.{ConsoleReader, History}
import org.aphreet.c3.platform.remote.api.RemoteException
import org.aphreet.c3.platform.management.cli.command.CommandFactory

class ManagementClient(override val args:Array[String]) extends CLI(args) {

  var connectionProvider:ConnectionProvider = null

  val clientName = "Shell"

  var debugOutput = false

  {
    LogManager.getLogManager.readConfiguration(getClass.getClassLoader.getResourceAsStream("log.properties"))

    if(cli.getOptions.length == 0) helpAndExit(clientName)

    if(cli.hasOption("help")) helpAndExit(clientName)

    if(cli.hasOption("ignoreSSLHostname")) disableHostNameVerification()

    if(cli.hasOption("debug")) debugOutput = true

    val connectionType = cliValue("t", "ws").toLowerCase

    connectionProvider = connectionType match {
      case "rmi" => new RmiConnectionProvider
      case "ws" => {
        val host = cliValue("h", "http://localhost:9301")
        val user = cliValue("u", "")
        val password = {
          print("Password:")
          val passwordReader = new ConsoleReader()
          passwordReader.setUseHistory(false)
          passwordReader.readLine(new java.lang.Character('*'))
        }
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
    "t" has mandatory argument "type" described "Connection type WS|RMI. Default is WS",
    "h" has mandatory argument "hostname" described "Host to connect to. Only for WS. Default is http://localhost:80",
    "u" has mandatory argument "username" described "Only for WS",
    "debug" described "Show debug output",
    "ignoreSSLHostname" described  "Ignore host name verification error",
    "help" described "Prints this message"
  )

  def run() {
    val reader = new ConsoleReader
    reader.setUseHistory(true)

    val history = new History(File.createTempFile("cli.history", ""))
    reader.setHistory(history)

    println("C3 Client version " + VersionUtils.clientVersion)

    val commandFactory = new CommandFactory(List(CommonCommands, SearchCommands, PrepareForReplication))

    var commandEvaluator = connect(reader)

    println("Welcome to C3 shell")
    print("C3>")


    while(true){
      val line = reader.readLine.trim

      var success = false

      for(i <- 1 to 5 if !success){
        success = commandFactory.getCommand(line) match {
          case Some(commandExecution) => {
            try{
              printResult(commandEvaluator.evaluate(commandExecution))
            }catch{
              case e:RemoteConnectFailureException=> {
                println("Connection to server lost. Trying to reconnect...")

                commandEvaluator = connect(reader)
                false
              }
              case e:RemoteException =>
                printResult(e.getMessage, e)
              case e =>{
                printResult("Failed to execute command: " + e.getClass.getSimpleName + " " + e.getMessage, e)
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

  def printResult(message:String, e:Throwable = null):Boolean = {
    println(message)
    if(e != null && debugOutput){
      e.printStackTrace()
    }
    true
  }

  def createCommandEvaluator(reader:ConsoleReader):CommandEvaluator = {
    new CommandEvaluator(reader, connectionProvider.access, connectionProvider.management)
  }

  def connect(reader:ConsoleReader):CommandEvaluator = {
    for(i <- 1 to 5){
      try{
        return createCommandEvaluator(reader)
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

  def disableHostNameVerification() {
    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
      new javax.net.ssl.HostnameVerifier() {
        override def verify(hostname: String, sslSession: javax.net.ssl.SSLSession): Boolean = true
      }
    )
  }
}