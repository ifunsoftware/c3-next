package org.aphreet.c3.platform.client.sanity

import com.ifunsoftware.c3.access.{DataStream, C3SystemFactory}
import scala.util.Random
import scala.collection.mutable.ArrayBuffer
import org.apache.commons.io.IOUtils._
import java.nio.channels.Channels
import com.twmacinta.util.MD5InputStream
import org.junit.Assert._

/**
 * Author: Mikhail Malygin
 * Date:   1/10/14
 * Time:   11:33 PM
 */
trait AccessOperations {

  import com.ifunsoftware.c3.access.C3System._

  val host = "http://localhost:8080"

  val domain = "anonymous"

  val key = ""

  def createSystem = new C3SystemFactory().createSystem(host, domain, key)

  def write(count: Int, size: Int, contentType: String = "application/octet-stream"): List[String] = {

    val addresses = new ArrayBuffer[String]()

    val system = createSystem

    val random = new Random()

    for(number <- 1 to count){

      val data = new Array[Byte](size)
      random.nextBytes(data)
      val address = system.addResource(Map("content-type" -> contentType), DataStream(data))

      addresses += address
    }

    addresses.toList
  }

  def readStrict(addresses: List[String]){

    val system = createSystem

    for(address <- addresses){
      val resource = system.getResource(address)

      val hash = resource.versions(0).hash
      val length = resource.versions(0).length.toInt

      val data = new Array[Byte](length)

      val md5Is = new MD5InputStream(Channels.newInputStream(system.getData(address)))

      readFully(md5Is, data)

      assertEquals(hash, md5Is.getMD5.asHex())

      closeQuietly(md5Is)
    }
  }

  def read(addresses: List[String]): Int = {
    val system = createSystem

    var read = 0

    for(address <- addresses){
      try{
        val resource = system.getResource(address)

        val length = resource.versions(0).length.toInt

        val data = new Array[Byte](length)

        val is = Channels.newInputStream(system.getData(address))

        readFully(is, data)

        closeQuietly(is)
        read = read + 1
      }catch{
        case e: Throwable =>
      }
    }

    read
  }

  def delete(addresses: List[String]): Int = {
    val system = createSystem

    var deleted = 0

    for(address <- addresses){
      try{
        
        system.deleteResource(address)
        
        deleted = deleted + 1
      }catch{
        case e: Throwable =>
      }
    }

    deleted
  }

}
