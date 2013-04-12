package org.aphreet.c3.platform.search.impl.index.extractor

import java.io._
import java.net.{HttpURLConnection, URL}
import java.nio.file.{StandardCopyOption, Files}
import org.aphreet.c3.platform.common.{Logger, Disposable}
import org.aphreet.c3.platform.common.Disposable._
import org.aphreet.c3.platform.resource.{DataStream, Resource}
import org.aphreet.c3.platform.search.impl.index.TextExtractor
import scala.Some
import scala.collection.JavaConversions._
import scala.language.reflectiveCalls
import scala.language.implicitConversions

class TikaHttpTextExtractor(val tikaHostName: String) extends TextExtractor {

  val log = Logger(getClass)

  def extract(resource: Resource): Option[ExtractedDocument] = {

    val contentType = resource.contentType

    callTika(resource.address, resource.versions.last.data, contentType)
  }

  def callTika(address:String, data: DataStream, contentType: String): Option[ExtractedDocument] = {

    using(openConnection(address))(connection => {

      using(connection.getOutputStream)(os => {

        data.writeTo(os)

        using(connection.getInputStream)(is => {
          connection.getResponseCode match {
            case HttpURLConnection.HTTP_OK => {

              val headersMap = (for (field <- mapAsScalaMap(connection.getHeaderFields)
                                     if (field._1 != null
                                       && field._1.startsWith("x-tika-extracted_")
                                       && !field._2.isEmpty))
              yield (field._1.replaceFirst("x-tika-extracted_", "").toLowerCase,
                  collectionAsScalaIterable(field._2).mkString(","))).filter(!_._2.isEmpty).toMap

              val path = Files.createTempFile("extracted", "tmp")

              Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING)

              if(isSmallEnough(path.toFile.length())){
                Some(new TikaExtractedDocument(path.toFile, headersMap))
              }else{
                log.info("Content of the resource " + address + " was skipped due to too long length: " + path.toFile.length())
                Files.deleteIfExists(path)
                None
              }
            }
            case _ => None
          }
        })
      })
    })
  }

  def openConnection(address: String): HttpURLConnection = {
    val connection = new URL(tikaHostName).openConnection().asInstanceOf[HttpURLConnection]

    connection.setDoInput(true)
    connection.setDoOutput(true)
    connection.setRequestMethod("POST")
    connection.setRequestProperty("c3.address", address)

    connection.connect()

    connection
  }

  implicit def disconnectToDisposable[S](what: S {def disconnect()}):Disposable[S] = {
    new Disposable[S](){
      override def dispose(){
        try{
          what.disconnect()
        }catch{
          case e: Throwable =>
        }
      }
    }
  }

  protected def isSmallEnough(length: Long): Boolean = {
    length < 5L * 1024 * 1024
  }
}

class TikaExtractedDocument(val file: File, val metadata :Map[String, String]) extends ExtractedDocument {

  lazy val content: String = new String(Files.readAllBytes(file.toPath), "UTF-8")

  def dispose() {
    Files.deleteIfExists(file.toPath)
  }
}

class TikaPlainTextDocument(val content: String) extends ExtractedDocument {

  def metadata = Map()

  def dispose() {}
}
