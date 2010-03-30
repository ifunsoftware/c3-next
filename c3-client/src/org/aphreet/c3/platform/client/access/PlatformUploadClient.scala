package org.aphreet.c3.platform.client.access

import http.C3HttpAccessor
import org.aphreet.c3.platform.client.common.CLI
import org.aphreet.c3.platform.client.common.ArgumentType._
import java.io.File

/**
 * Created by IntelliJ IDEA.
 * User: malygm
 * Date: Mar 30, 2010
 * Time: 3:33:54 PM
 * To change this template use File | Settings | File Templates.
 */

class PlatformUploadClient(override val args:Array[String]) extends CLI(args){

  def cliDescription = parameters(
    "host" has mandatory argument "host" described "Host to connect to",
    "pool" has mandatory argument "name" described "Target pool",
    "out" has mandatory argument "file" described "File to write resource addressed",
    "file" has mandatory argument "path" described "File or Directory path to upload",
    "help" described "Prints this message"
  )

  def run{
    if(cli.getOptions.length == 0) helpAndExit("Uploader")

    if(cli.hasOption("help")) helpAndExit("Uploader")

    val pool = cliValue("pool", "")
    val host = "http://" + cliValue("host", "localhost:8088") + "/c3-remote/"
    val out = cliValue("out", null)
    val path = cliValue("file", null)

    if(path == null){
      throw new IllegalArgumentException("file argument required")
    }

    val file = new File(path)
    if(!file.exists){
      throw new IllegalArgumentException("Specified file does not exit")
    }
  }

  def upload(host:String, pool:String, path:File, out:String){

    println("Starting  upload...")

    val client = new C3HttpAccessor(host)

    var fileList:List[File] = List()


    if(path.isDirectory){
      print("Creating file list...")
      fileList = getFullFileList(path)
      println("Done")
    }else{
      fileList = List(path)
    }

    fileList.foreach(
      uploadFile(client, _, pool)
    )

    println("Upload complete")
  }

  def uploadFile(client:C3HttpAccessor, path:File, pool:String):String = {

    print("Uploading " + path.getName + "...")


    val metadata:Map[String, String] = Map("pool" -> pool, "origname" -> path.getName)
    try{
      val ra = client.upload(path, metadata)
      println("Done")
      ra
    }catch{
      case e=> println("Error")
      null
    }
    
  }

  private def getFullFileList(dir:File):List[File] = {
    var result:List[File] = List()
    for(file <- dir.listFiles)
      if(file.isDirectory){
        result = result ::: getFullFileList(file)
      }else{
        result = file :: result
      }
    result
  }
}