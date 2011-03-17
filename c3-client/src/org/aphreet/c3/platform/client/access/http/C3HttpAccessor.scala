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

package org.aphreet.c3.platform.client.access.http

import org.apache.commons.httpclient.methods.multipart._
import org.apache.commons.httpclient.methods.{DeleteMethod, GetMethod, PostMethod}
import java.nio.channels.Channels
import java.io._
import java.nio.ByteBuffer
import org.apache.commons.httpclient.{Header, HttpMethodBase, HttpStatus, HttpClient}
import com.twmacinta.util.MD5
import xml.XML

class C3HttpAccessor(val host:String, override val domain:String, override val secret:String)
    extends AbstractHttpAccessor(domain, secret){

  val requestUri = "/rest/resource/"
  val url = host + requestUri

  val httpClient = new HttpClient



  def write(data:Array[Byte], metadata:Map[String, String]):String =
    writeData(new FilePart("data", new ByteArrayPartSource(data)), metadata)



  def upload(file:File, metadata:Map[String, String]):String =
    writeData(new FilePart("data", new FilePartSource(file)), metadata)



  private def writeData(filePart:FilePart, metadata:Map[String, String]):String = {
    val postMethod = new PostMethod(url)

    addAuthHeader(postMethod, requestUri)

    val parts:Array[Part] = (filePart ::
            metadata.map(e => {
              val part = new StringPart(e._1, e._2, "UTF-16")
              part.setCharSet("UTF-8")
              part
            }).toList).toArray

    val entity = new MultipartRequestEntity(parts, postMethod.getParams)

    postMethod.setRequestEntity(new MultipartRequestEntity(parts, postMethod.getParams))

    try{
      val status = httpClient.executeMethod(postMethod)
      status match {
        case HttpStatus.SC_CREATED => {

          val xml = XML.load(postMethod.getResponseBodyAsStream)

          (xml \\ "uploaded")(0) \ "@address" text

        }
        case _ =>
          println(postMethod.getResponseBodyAsString)
          throw new Exception(("Filed to post resource, code " + status).asInstanceOf[String])
      }
    }finally {
      postMethod.releaseConnection
    }
  }



  def downloadData(address:String, file:File) = {
    val getMethod = new GetMethod(url + address)

    addAuthHeader(getMethod, requestUri + address)

    try{
      val status = httpClient.executeMethod(getMethod)
      status match {
        case HttpStatus.SC_OK => {
          val fileChannel = new FileOutputStream(file).getChannel
          val inChannel = Channels.newChannel(new BufferedInputStream(getMethod.getResponseBodyAsStream))
          try{
            fileChannel.transferFrom(inChannel, 0, getMethod.getResponseContentLength)
          }finally{
            fileChannel.close
            inChannel.close
          }
        }
        case _ => throw new Exception(("Failed to get resource, code " + status).asInstanceOf[String])
      }
    }finally{
      getMethod.releaseConnection();
    }
  }



  def downloadMD(address:String, file:File) = {
    val getMethod = new GetMethod(url + address + "?metadata")

    addAuthHeader(getMethod, requestUri + address + "?metadata")

    try{
      val status = httpClient.executeMethod(getMethod)
      status match {
        case HttpStatus.SC_OK => {
          val fileChannel = new FileOutputStream(file).getChannel

          try{
            fileChannel.write(ByteBuffer.wrap(getMethod.getResponseBody))
          }finally{
            fileChannel.close
          }
        }
        case _ => throw new Exception(("Failed to get resource, code " + status).asInstanceOf[String])
      }
    }finally{
      getMethod.releaseConnection();
    }
  }



  def delete(address:String) = {
    val deleteMethod = new DeleteMethod(url + address)

    addAuthHeader(deleteMethod, requestUri + address)

    try{
      val status = httpClient.executeMethod(deleteMethod)
      status match{
        case HttpStatus.SC_OK => null
        case _ => throw new Exception(("Failed to delete resource, code " + status).asInstanceOf[String])
      }
    }
  }



  def fakeRead(address:String):Int = {
    val getMethod = new GetMethod(url + address)

    addAuthHeader(getMethod, requestUri + address)

    try{
      val status = httpClient.executeMethod(getMethod)
      return status match {
        case HttpStatus.SC_OK => {
          val stream = getMethod.getResponseBodyAsStream
          var read = 0
          while(stream.read != -1){read = read + 1}
          read
        }
        case _ =>
          println(getMethod.getResponseBodyAsString)
          throw new Exception(("Failed to get resource, code " + status).asInstanceOf[String])
      }
    }finally{
      getMethod.releaseConnection();
    }
  }
}

class ByteArrayPartSource(val data:Array[Byte]) extends PartSource {

  override def createInputStream:InputStream = {
    new ByteArrayInputStream(data)
  }

  override def getFileName:String = {
    return "array"
  }

  override def getLength:Long = data.length
}