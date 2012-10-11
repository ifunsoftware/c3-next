package org.aphreet.c3.platform.backup.impl

import org.aphreet.c3.platform.common.{Path => C3Path, CloseableIterable, CloseableIterator}
import org.aphreet.c3.platform.resource.{PathDataStream, ResourceSerializer, Resource}
import java.nio.file._
import java.util
import java.net.URI
import java.nio.charset.Charset
import scala.collection.JavaConversions._

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
    Files.write(jsonFile, ResourceSerializer.toJSON(resource, true).getBytes("UTF-8"), StandardOpenOption.CREATE_NEW)

    for ((version, number) <- resource.versions.view.zipWithIndex){
      val dataFile = zipFs.getPath(dirName, address + "." + number)
      version.data.writeTo(dataFile)
    }

    Files.write(zipFs.getPath("list"), (resource.address + "\n").getBytes("UTF-8"), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
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
