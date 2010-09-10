package org.aphreet.c3.platform.client.access

import org.aphreet.c3.platform.client.access.http.C3HttpAccessor
import org.aphreet.c3.platform.client.common.CLI
import java.io.{OutputStream, BufferedOutputStream, FileOutputStream, File}

class PlatformUploadClient(override val args:Array[String]) extends CLI(args){

  def clientName = "Uploader"

  def cliDescription = parameters(
    HOST_ARG,
    POOL_ARG,
    OUT_ARG,
    FILE_ARG,
    USER_ARG,
    KEY_ARG,
    HELP_ARG
  )

  def run{    
    val path = cliValue(FILE_ARG)

    if(path == null){
      throw new IllegalArgumentException("file argument required")
    }

    val file = new File(path)
    if(!file.exists){
      throw new IllegalArgumentException("Specified file does not exit")
    }


    upload(HOST_ARG, USER_ARG, KEY_ARG, POOL_ARG, file, OUT_ARG)
  }

  def upload(host:String, user:String, key:String, pool:String, path:File, out:String){

    var fos:OutputStream = null

    if(out != null){

      val fileHandle = new File(out)
      if(fileHandle.exists) fileHandle.delete

      fos = new BufferedOutputStream(new FileOutputStream(fileHandle))
    }

    println("Starting  upload...")

    val client = new C3HttpAccessor(host, user, key)

    var fileList:List[File] = List()


    if(path.isDirectory){
      print("Creating file list...")
      fileList = getFullFileList(path)
      println("Done")
    }else{
      fileList = List(path)
    }

    for(file <- fileList){
      val ra = uploadFile(client, file, pool)
      if(fos != null) fos.write((ra + "\n").getBytes)
    }

    if(fos != null) fos.close

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
      case e=> {
        println("Error")
        null
      }
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