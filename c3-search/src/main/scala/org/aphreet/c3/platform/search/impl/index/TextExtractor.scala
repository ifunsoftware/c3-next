package org.aphreet.c3.platform.search.impl.index

import org.apache.commons.logging.LogFactory
import org.aphreet.c3.search.tika.TikaProvider
import org.aphreet.c3.platform.search.impl.rmi.SearchRmiProxyFactoryBean
import java.io.{FileOutputStream, File}
import java.nio.channels.WritableByteChannel
import org.aphreet.c3.platform.resource.{FileDataStream, Resource}
import collection.JavaConversions._
/**
 * Copyright (c) 2010, Mikhail Malygin
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
 * 3. Neither the name of the IFMO nor the names of its contributors 
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

class TextExtractor{
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
          channel.close
        }
      }

      Map() ++ asMap(tikaProvider.extractMetadata(file.getCanonicalPath))

    }catch{
      case e=> log.warn("Failed to extract document content: " + e.getMessage)
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
    rmiBean.afterPropertiesSet
    log debug "Connected"
    rmiBean.getObject.asInstanceOf[TikaProvider]
  }
}