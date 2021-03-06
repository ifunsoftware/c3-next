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
import org.apache.commons.httpclient.methods._
import java.nio.channels.Channels
import java.io._
import java.nio.ByteBuffer
import org.apache.commons.httpclient.{Header, HttpMethodBase, HttpStatus, HttpClient}
import xml.XML
import java.text.SimpleDateFormat
import java.util.{Date, Locale}
import org.aphreet.c3.platform.client.common.HashUtil
import org.apache.commons.codec.binary.Base64

class C3HttpAccessor(val host:String, val domain:String, val secret:String){

  val requestUri = "/rest/resource/"
  val url = host + requestUri

  val httpClient = new HttpClient

  def write(data:Array[Byte], metadata:Map[String, String]):String =
    writeData(new ByteArrayRequestEntity(data), metadata)

  def upload(file:File, metadata:Map[String, String]):String =
    writeData(new FileRequestEntity(file, "application/octet-string"), metadata)

  private def writeData(requestEntity: RequestEntity, metadata:Map[String, String]):String = {
    val postMethod = new PostMethod(url)

    postMethod.setRequestEntity(requestEntity)

    metadata.foreach(e => postMethod.addRequestHeader("x-c3-metadata", e._1 + ":" + new String(Base64.encodeBase64(e._2.getBytes("UTF-8")))))

    addAuthHeader(postMethod, requestUri)

    try{
      val status = httpClient.executeMethod(postMethod)
      status match {
        case HttpStatus.SC_CREATED => {

          val xml = XML.load(postMethod.getResponseBodyAsStream)

          ((xml \\ "uploaded")(0) \ "@address").text

        }
        case _ =>
          println(postMethod.getResponseBodyAsString)
          throw new Exception("Failed to post resource, code " + status)
      }
    }finally {
      postMethod.releaseConnection()
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
            fileChannel.close()
            inChannel.close()
          }
        }
        case _ =>
          println(getMethod.getResponseBodyAsString)
          throw new Exception("Failed to get resource, code " + status)
      }
    }finally{
      getMethod.releaseConnection()
    }
  }



  def downloadMD(address:String, file:File) = {
    val getMethod = new GetMethod(url + address + "?metadata")

    addAuthHeader(getMethod, requestUri + address)

    try{
      val status = httpClient.executeMethod(getMethod)
      status match {
        case HttpStatus.SC_OK => {
          val fileChannel = new FileOutputStream(file).getChannel

          try{
            fileChannel.write(ByteBuffer.wrap(getMethod.getResponseBody))
          }finally{
            fileChannel.close()
          }
        }
        case _ =>
          println(getMethod.getResponseBodyAsString)          
          throw new Exception("Failed to get resource, code " + status)
      }
    }finally{
      getMethod.releaseConnection()
    }
  }

  def delete(address:String) = {
    val deleteMethod = new DeleteMethod(url + address)

    addAuthHeader(deleteMethod, requestUri + address)

    try{
      val status = httpClient.executeMethod(deleteMethod)
      status match{
        case HttpStatus.SC_OK => null
        case _ => throw new Exception("Failed to delete resource, code " + status)
      }
    }
  }

  def makeDir(path:String) {

    val postMethod = new PostMethod(url + path)

    addAuthHeader(postMethod, "/rest/fs" + path)
    postMethod.addRequestHeader("x-c3-nodetype", "directory")

    try{
      val status = httpClient.executeMethod(postMethod)
      status match {
        case HttpStatus.SC_CREATED => {

        }
        case _ => throw new Exception("Failed to make directory, message: " + postMethod.getResponseBodyAsString)
      }
    }
  }

  def fakeRead(address:String):Int = {
    val getMethod = new GetMethod(url + address)

    addAuthHeader(getMethod, requestUri + address)

    try{
      val status = httpClient.executeMethod(getMethod)
      status match {
        case HttpStatus.SC_OK => {
          val stream = getMethod.getResponseBodyAsStream
          var read = 0
          while(stream.read != -1){read = read + 1}
          read
        }
        case _ =>
          println(getMethod.getResponseBodyAsString)
          throw new Exception("Failed to get resource, code " + status)
      }
    }finally{
      getMethod.releaseConnection()
    }
  }

  def addAuthHeader(method:HttpMethodBase, resource:String) {
    if(domain != "anonymous"){

      val dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", new Locale("en_US"))

      val dateString = dateFormat.format(new Date())

      val hashBase = resource + dateString + domain

      val hash = HashUtil.hmac(secret, hashBase)

      val header = new Header("x-c3-sign", hash)
      method.addRequestHeader(header)

      val domainHeader = new Header("x-c3-domain", domain)
      method.addRequestHeader(domainHeader)

      val dateHeader = new Header("x-c3-date", dateString)
      method.addRequestHeader(dateHeader)
    }
  }
}

class ByteArrayPartSource(val data:Array[Byte]) extends PartSource {

  override def createInputStream:InputStream = {
    new ByteArrayInputStream(data)
  }

  override def getFileName:String = {
    "array"
  }

  override def getLength:Long = data.length
}