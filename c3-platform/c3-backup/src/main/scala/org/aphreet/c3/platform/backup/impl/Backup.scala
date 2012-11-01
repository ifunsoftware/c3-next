/*
 * Copyright (c) 2012, Mikhail Malygin
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
 * 3. Neither the name of the iFunSoftware nor the names of its contributors
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

package org.aphreet.c3.platform.backup.impl

import org.aphreet.c3.platform.common.{Path => C3Path, CloseableIterable, CloseableIterator}
import org.aphreet.c3.platform.resource.{PathDataStream, ResourceSerializer, Resource}
import java.nio.file._
import java.util
import java.net.URI
import java.nio.charset.Charset
import scala.collection.JavaConversions._
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

class Backup(val uri:URI, val create:Boolean) extends CloseableIterable[Resource] {

  var zipFs:FileSystem = null

  {
    val env = new util.HashMap[String, String]()
    env.put("create", create.toString)
    zipFs = FileSystems.newFileSystem(uri, env, null)
  }

  def addResource(resource:Resource){

    resource.embedData = false
    val address = resource.address
    val dir = Backup.directoryForAddress(zipFs, address)

    Files.createDirectories(dir)

    val dirName = dir.toString

    val binaryFile = zipFs.getPath(dirName, address + ".bin")
    Files.write(binaryFile, resource.toByteArray, StandardOpenOption.CREATE_NEW)

    val jsonFile = zipFs.getPath(dirName, address + ".json")
    Files.write(jsonFile, ResourceSerializer.toJSON(resource, full = true).getBytes("UTF-8"), StandardOpenOption.CREATE_NEW)

    for ((version, number) <- resource.versions.view.zipWithIndex){
      val dataFile = zipFs.getPath(dirName, address + "." + number)
      version.data.writeTo(dataFile)
    }

    Files.write(zipFs.getPath("list"), (resource.address + "\n").getBytes("UTF-8"), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
  }

  def writeFileSystemRoots(roots:Map[String, String]){
    val properties = new java.util.Properties()
    properties.putAll(mapAsJavaMap(roots))

    val bos = new ByteArrayOutputStream()
    properties.storeToXML(bos, "")

    Files.write(zipFs.getPath("fs.xml"), bos.toByteArray, StandardOpenOption.CREATE)

    bos.close()
  }

  def readFileSystemRoots:Map[String, String] = {

    val properties = new java.util.Properties()

    val is = new ByteArrayInputStream(Files.readAllBytes(zipFs.getPath("fs.xml")))
    properties.loadFromXML(is)
    is.close()

    mapAsScalaMap(properties).toMap.asInstanceOf[Map[String, String]]
  }

  def close(){
    zipFs.close()
  }

  private lazy val addresses:Seq[String] = Files.readAllLines(zipFs.getPath("list"), Charset.forName("UTF-8"))

  override def iterator = new BackupIterator(zipFs, addresses)

  override def size:Int = addresses.size

}

class BackupIterator(val zipFs: FileSystem, val addresses:Seq[String]) extends CloseableIterator[Resource]{

  val addressIterator = addresses.iterator

  def hasNext = addressIterator.hasNext

  def next():Resource = {

    val address = addressIterator.next()

    val dir = Backup.directoryForAddress(zipFs, address)
    val binaryResource = Files.readAllBytes(zipFs.getPath(dir.toString, address + ".bin"))

    val resource = Resource.fromByteArray(binaryResource)

    for ((version, number) <- resource.versions.view.zipWithIndex){
      version.setData(new PathDataStream(zipFs.getPath(dir.toString, address + "." + number)))
    }

    resource
  }

  def close(){
    zipFs.close()
  }
}

trait ResourceConsumer{

  def consume(resource:Resource)

}

object Backup{


  def open(path:C3Path):Backup = {
    val zipFile = URI.create("jar:file:" + path)
    new Backup(zipFile, false)
  }

  def create(path:C3Path):Backup = {

    val zipFile = URI.create("jar:file:" + path )

    new Backup(zipFile, true)
  }

  def directoryForAddress(fs:FileSystem, address:String):Path = {
    val firstLetter = address.charAt(0).toString
    val secondLetter = address.charAt(1).toString
    val thirdLetter = address.charAt(2).toString

    fs.getPath(firstLetter, secondLetter, thirdLetter)
  }

}
