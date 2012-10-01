package org.aphreet.c3.platform.backup.impl

import org.aphreet.c3.platform.common.{Path => C3Path}
import org.aphreet.c3.platform.resource.{ResourceSerializer, Resource}
import java.nio.file._
import java.util
import java.net.URI

class Backup(val uri:URI) {

  var zipFs:FileSystem = null

  {
    val env = new util.HashMap[String, String]()
    env.put("create", "true")
    zipFs = FileSystems.newFileSystem(uri, env, null)
  }

  def addResource(resource:Resource){

    val address = resource.address
    val firstLetter = address.charAt(0).toString
    val secondLetter = address.charAt(1).toString
    val thirdLetter = address.charAt(2).toString

    val dir = zipFs.getPath(firstLetter, secondLetter, thirdLetter)

    Files.createDirectories(dir)

    val dirName = dir.toString

    val binaryFile = zipFs.getPath(dirName, address + ".bin")
    Files.write(binaryFile, resource.toByteArray, StandardOpenOption.CREATE_NEW)

    val jsonFile = zipFs.getPath(dirName, address + ".json")
    Files.write(jsonFile, ResourceSerializer.toJSON(resource, true).getBytes("UTF-8"), StandardOpenOption.CREATE_NEW)

    for(versionNumber <- 0 to resource.versions.length - 1){
      val dataFile = zipFs.getPath(dirName, address + "." + versionNumber)
      resource.versions(versionNumber).data.writeTo(dataFile)
    }

  }


  def close(){
    zipFs.close()
  }

}

object Backup{

  def create(path:C3Path):Backup = {

    val zipFile = URI.create("jar:file:" + path + "/backup.zip")

    new Backup(zipFile)

  }

}
