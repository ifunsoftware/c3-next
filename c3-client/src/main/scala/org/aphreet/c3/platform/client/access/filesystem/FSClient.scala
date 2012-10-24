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

package org.aphreet.c3.platform.client.access.filesystem

import org.aphreet.c3.platform.client.access.http.{C3SearchAccessor, C3FileHttpAccessor, C3HttpAccessor}
import org.aphreet.c3.platform.client.common.{VersionUtils, CLI}
import org.aphreet.c3.platform.client.common.ArgumentType._
import java.io.{FileOutputStream, File, InputStreamReader, BufferedReader}

class FSClient(override val args:Array[String]) extends CLI(args){

  val clientName = "FS"

  var currentDir = "/"

  var resourceAccessor:C3HttpAccessor = _
  var fileAccessor:C3FileHttpAccessor = _
  var searchAccessor:C3SearchAccessor = _

  def cliDescription = parameters(
    "h" has mandatory argument "hostname" described "Host to connect to",
    "u" has mandatory argument "username" described "Username used to access to C3",
    "s" has mandatory argument "secret" described "Secret key",
    "ignoreSSLHostname" described  "Ignore host name verification error",
    "help" described "Prints this message"
  )

  def run() {
    
    if(cli.hasOption("help")) helpAndExit(clientName)

    if(cli.hasOption("ignoreSSLHostname")) disableHostNameVerification()

    val host = cliValue("h", "http://localhost:7373")
    val user = cliValue("u", "anonymous")
    val secret = cliValue("s", "")

    resourceAccessor = new C3HttpAccessor(host, user, secret)
    fileAccessor = new C3FileHttpAccessor(host, user, secret)
    searchAccessor = new C3SearchAccessor(host, user, secret)

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
        case "upload" => upload(list.tail)
        case "download" => download(list.tail)
        case "cd" => cd(list.tail)
        case "rm" => rm(list.tail)
        case "setmd" => setmd(list.tail)
        case "exit" => System.exit(0)
        case "help" => help()
        case "mv" => mv(list.tail)
        case "search" => search(list.tail)

        case _ => println("Unknown command. Type help to show available commands")
      }


      print("C3:" + currentDir + "$")
    }
  }

  def help(){
    println("cd <path>                           - Change current directory")
    println("download <remote file> <local file> - download remote file content")
    println("exit                                - Exit from shell")
    println("help                                - Print this message")
    println("info [<path>]                       - Download file metadata")
    println("mkdir <path>                        - Create new directory")
    println("mv <path> <new path>                - Rename or move file")
    println("rm <path>                           - Remove file or directory")
    println("setmd <path> <key> <value>          - Set metadata key/value")
    println("upload <remote file> <local file>   - Upload file to c3")
    println("search <query>                      - Search for files in the current domain")
  }

  def mv(args:List[String]) {
    val name = workDir(args)

    val newName = args.tail.head

    fileAccessor.moveFile(name, newName)
  }

  def setmd(args:List[String]) {
    val directory = workDir(args)

    val key = args.tail.head
    val value = args.tail.tail.head

    fileAccessor.updateFile(directory, null, Map[String, String]((key, value)))

  }

  def rm(args:List[String]) {
    val directory = workDir(args)
    fileAccessor.delete(directory)
  }

  def ls(args:List[String]) {

    val directory = workDir(args)

    if(!isDirectory(directory)){
      println(directory + " is not a directory")
      return
    }

    val directoryData = fileAccessor.getNodeDataAsXml(directory)

    val nodes = ((directoryData \\ "directory")(0) \\ "nodes")(0) \\ "node"

    for(node <- nodes){
      val name = (node \ "@name").text
      val isFile = if((((node \ "@leaf") text) toBoolean)){
        "f"
      }else{
        "d"
      }
      println(isFile + " " + name)
    }
  }

  def cd(args:List[String]) {

    val directory = workDir(args)

    if(isDirectory(directory)){
      currentDir = directory

      if(!currentDir.endsWith("/")){
        currentDir = currentDir + "/"
      }

    }else{
      println("can't cd, specified path is not a directory")
    }

  }

  def mkdir(args:List[String]) {

    fileAccessor.makeDir(workDir(args))
  }

  def upload(args:List[String]) {

    val directory = workDir(args)
    val fileToUpload = new File(args.tail.head)

    println(fileToUpload.getAbsolutePath)
    println(directory)

    fileAccessor.uploadFile(directory, fileToUpload)
  }

  def download(args:List[String]) {

    val directory = workDir(args)

    args.tail.headOption match{
      case Some(x) =>

        val fos = new FileOutputStream(new File(x))
        fos.write(fileAccessor.getNodeData(directory))
        fos.close()

      case None => val directoryData = new String(fileAccessor.getNodeData(directory), "UTF-8")

      println(directoryData)
    }
  }

  def search(args:List[String]) {
    args.headOption match {
      case Some(value) => {
        val xml = searchAccessor.search(value)

        (xml \\ "entry").foreach(e => {
          println(e \ "@address" + " : " + e \ "@score")
          (e \ "@path").headOption match {
            case Some(pathAttribute) => println(pathAttribute.text)
            case None =>
          }
          (e \\ "fragment").foreach(f => {
            println(" -> " + f \ "@field")

            (f \\ "string").foreach(s => println("\t\t" + s.text))
          })
        })
      }
      case None => println("query string required")
    }
  }

  def info(args:List[String]) {
    val metadata = fileAccessor.getNodeMetadata(workDir(args))
    println(metadata)
  }

  def isDirectory(nodePath:String):Boolean = {

    val xml = fileAccessor.getNodeMetadataAsXML(nodePath)

    val mdElements = ((xml \\ "resource")(0) \\ "systemMetadata")(0) \\ "element"

    for(element <- mdElements){
      if(((element \ "@key") text) == "c3.fs.nodetype"){
        val nodeType = (element \\ "value")(0).text

        return nodeType == "directory"
      }
    }

    throw new RuntimeException("Failed to find node type")
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

  def disableHostNameVerification() {
    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
      new javax.net.ssl.HostnameVerifier() {
        override def verify(hostname: String, sslSession: javax.net.ssl.SSLSession): Boolean = true
      }
    )
  }
}