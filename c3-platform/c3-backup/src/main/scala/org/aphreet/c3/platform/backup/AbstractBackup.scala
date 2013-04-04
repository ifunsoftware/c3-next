package org.aphreet.c3.platform.backup

import impl.Backup
import org.aphreet.c3.platform.common.{Logger, CloseableIterator, CloseableIterable}
import org.aphreet.c3.platform.resource.{PathDataStream, ResourceSerializer, Resource}
import java.nio.file._
import collection.JavaConversions._
import java.io._
import java.nio.charset.Charset
import org.apache.commons.codec.digest.DigestUtils
import java.nio.file.FileSystem
import org.aphreet.c3.platform.common.Disposable._


abstract class AbstractBackup extends CloseableIterable[Resource] {

  var zipFs:FileSystem = null
  var zipFilePath : String = null
  var md5FilePath : String = null

  val log = Logger(getClass)


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

      using(new FileInputStream(zipFilePath))(is => {
        using(new PrintWriter(new FileWriter(md5FilePath)))(pw => {
          pw.println(DigestUtils.md5Hex(is))
        })
      })
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

