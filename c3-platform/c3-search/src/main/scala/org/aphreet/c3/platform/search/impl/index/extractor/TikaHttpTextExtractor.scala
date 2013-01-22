package org.aphreet.c3.platform.search.impl.index.extractor

import java.net.{HttpURLConnection, URL}
import org.aphreet.c3.platform.common.Disposable
import org.aphreet.c3.platform.common.Disposable._
import org.aphreet.c3.platform.resource.{DataStream, Resource}
import scala.collection.JavaConversions._
import java.io._
import java.nio.file.{StandardCopyOption, Files}
import org.aphreet.c3.platform.search.impl.index.TextExtractor
import org.springframework.stereotype.Component
import scala.Some

@Component
class TikaHttpTextExtractor extends TextExtractor {

  def extract(resource: Resource): Option[ExtractedDocument] = {
    callTika(resource.versions.last.data, resource.metadata.getOrElse("content.type", "application/octet-stream"))
  }

  def callTika(data: DataStream, contentType: String): Option[ExtractedDocument] = {

    using(openConnection())(connection => {

      using(connection.getOutputStream)(os => {

        data.writeTo(os)

        using(connection.getInputStream)(is => {
          connection.getResponseCode match {
            case HttpURLConnection.HTTP_OK => {

              val headersMap = (for (field <- mapAsScalaMap(connection.getHeaderFields)
                                     if (field._1 != null
                                       && field._1.startsWith("x-tika-extracted_")
                                       && !field._2.isEmpty))
              yield (field._1.replaceFirst("x-tika-extracted_", ""),
                  collectionAsScalaIterable(field._2).mkString(","))).filter(!_._2.isEmpty).toMap

              val path = Files.createTempFile("extracted", "tmp")

              Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING)
              headersMap + (("content" -> ""))

              Some(new TikaExtractedDocument(path.toFile, headersMap))
            }
            case _ => None
          }
        })
      })
    })
  }

  def openConnection(): HttpURLConnection = {
    val connection = new URL("https://tika-ifunsoftware.rhcloud.com").openConnection().asInstanceOf[HttpURLConnection]

    connection.setDoInput(true)
    connection.setDoOutput(true)
    connection.setRequestMethod("POST")

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

}

class TikaExtractedDocument(val file: File, val metadata :Map[String, String]) extends ExtractedDocument {

  private var readers = List[Reader]()

  private def openReader: Reader = {
    this.synchronized{
      readers = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")) :: readers
      readers.head
    }
  }

  def contentReader = openReader



  def dispose() {
    readers.foreach(_.close())
    Files.deleteIfExists(file.toPath)
  }
}
