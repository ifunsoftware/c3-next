/**
 * Copyright (c) 2011, Mikhail Malygin
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

package org.aphreet.c3.platform.client.access

import http.{C3FileHttpAccessor, C3HttpAccessor}
import java.io.{InputStreamReader, BufferedReader}
import org.aphreet.c3.platform.client.common.{VersionUtils, CLI}
import org.aphreet.c3.platform.client.common.ArgumentType._

class FSClient(override val args:Array[String]) extends CLI(args){

  val clientName = "FS"

  var currentDir = "/"

  var resourceAccessor:C3HttpAccessor = _
  var fileAccessor:C3FileHttpAccessor = _

  def cliDescription = parameters(
    "h" has mandatory argument "hostname" described "Host to connect to",
    "u" has mandatory argument "username" described "Username used to access to C3",
    "s" has mandatory argument "secret" described "Secret key",
    "ignoreSSLHostname" described  "Ignore host name verification error",
    "help" described "Prints this message"
  )

  def run = {
    
    if(cli.hasOption("help")) helpAndExit(clientName)

    if(cli.hasOption("ignoreSSLHostname")) disableHostNameVerification

    val host = cliValue("h", "http://localhost:7373")
    val user = cliValue("u", "anonymous")
    val secret = cliValue("s", "")

    resourceAccessor = new C3HttpAccessor(host, user, secret)
    fileAccessor = new C3FileHttpAccessor(host, user, secret)

    val reader = new BufferedReader(new InputStreamReader(System.in))

    println("C3 Filesystem client version " + VersionUtils.clientVersion)
    
    println("Welcome to C3 filesystem browser")

    print("C3:" + currentDir + "$")


    while(true){
      val line = reader.readLine.trim


      val list = line.split("\\s+").toList

      list.head match{
        case "ls" => ls(list.tail)
        case "mkdir" => mkdir(list.tail)
        case "info"  => info(list.tail)
        case "exit" => System.exit(0)

        case _ => println("Unknown command.")
      }


      print("C3:" + currentDir + "$")
    }
  }



  def ls(args:List[String]) = {

    val directoryData = new String(fileAccessor.getNodeData(workDir(args)), "UTF-8")

    println(directoryData)
  }

  def mkdir(args:List[String]) = {

    fileAccessor.makeDir(workDir(args))
  }

  def info(args:List[String]) = {
    val metadata = fileAccessor.getNodeMetadata(workDir(args))
    println(metadata)
  }

  def workDir(args:List[String]):String = {
    var directoryToList = currentDir

    if(!args.isEmpty){
      if(args.head.startsWith("/")){
        directoryToList = args.head
      }else{
        directoryToList = currentDir + args.head
      }
    }

    directoryToList
  }

  def disableHostNameVerification = {
    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
      new javax.net.ssl.HostnameVerifier() {
        override def verify(hostname: String, sslSession: javax.net.ssl.SSLSession): Boolean = true
      }
    )
  }
}