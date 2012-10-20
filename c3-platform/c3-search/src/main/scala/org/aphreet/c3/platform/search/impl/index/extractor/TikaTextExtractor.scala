package org.aphreet.c3.platform.search.impl.index.extractor

import org.apache.commons.logging.LogFactory
import org.aphreet.c3.search.tika.TikaProvider
import org.aphreet.c3.platform.resource.{FileDataStream, Resource}
import java.io.{FileOutputStream, File}
import java.nio.channels.WritableByteChannel
import collection.JavaConversions._
import org.aphreet.c3.platform.search.impl.rmi.SearchRmiProxyFactoryBean
import org.aphreet.c3.platform.search.impl.index.TextExtractor

class TikaTextExtractor extends TextExtractor{
  val log = LogFactory.getLog(getClass)

  var tikaProvider:TikaProvider = null

  def extract(resource:Resource):Map[String,String] = {

    var file:File = null
    var shouldDelete = false

    try{
      if(tikaProvider == null) tikaProvider = connect

      val stream = resource.versions.last.data

      if(stream.isInstanceOf[FileDataStream]){
        file = stream.asInstanceOf[FileDataStream].file
      }else{
        file = File.createTempFile("index", "tmp")
        shouldDelete = true
        var channel:WritableByteChannel = null
        try{
          channel = new FileOutputStream(file).getChannel
          stream.writeTo(channel)
        }finally{
          channel.close()
        }
      }

      mapAsScalaMap(tikaProvider.extractMetadata(file.getCanonicalPath)).toMap

    }catch{
      case e: Throwable => log.warn("Failed to extract document content: " + e.getMessage)
      log.trace("Cause: ", e)
      tikaProvider = null
      Map()
    }finally {
      if(shouldDelete && file != null){
        file.delete
      }
    }
  }

  def connect:TikaProvider = {
    log debug "Connecting to tika..."
    val rmiBean = new SearchRmiProxyFactoryBean
    rmiBean.setServiceUrl("rmi://localhost:1399/TikaProvider")
    rmiBean.setServiceInterface(classOf[TikaProvider])
    rmiBean.afterPropertiesSet()
    log debug "Connected"
    rmiBean.getObject.asInstanceOf[TikaProvider]
  }
}