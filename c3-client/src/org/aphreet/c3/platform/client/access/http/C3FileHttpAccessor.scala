/**
 * Copyright (c) 2011, Mikhail Malygin
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

import com.twmacinta.util.MD5
import org.apache.commons.httpclient.{HttpStatus, Header, HttpMethodBase, HttpClient}
import org.apache.commons.httpclient.methods.multipart._
import java.io.{InputStream, File, FileOutputStream}
import xml.{XML, Elem}
import org.apache.commons.httpclient.methods.{PutMethod, DeleteMethod, PostMethod, GetMethod}

class C3FileHttpAccessor(val host:String, val username:String, val key:String){

  val requestUri = "/rest/fs"
  val url = host + requestUri

  val httpClient = new HttpClient

  def getNodeData(path:String):Array[Byte] = {

    val getMethod = new GetMethod(url + path)

    addAuthHeader(getMethod, requestUri + path)

    try{
      val status = httpClient.executeMethod(getMethod)
      status match {
        case HttpStatus.SC_OK => {
         getMethod.getResponseBody
        }
        case _ =>
          println(getMethod.getResponseBodyAsString)
          throw new Exception(("Failed to get resource, code " + status).asInstanceOf[String])
      }
    }finally{
      getMethod.releaseConnection();
    }
  }

  def getNodeDataAsXml(path:String):Elem = {

    val getMethod = new GetMethod(url + path)

    addAuthHeader(getMethod, requestUri + path)

    try{
      val status = httpClient.executeMethod(getMethod)
      status match {
        case HttpStatus.SC_OK => {
         XML.load(getMethod.getResponseBodyAsStream)
        }
        case _ =>
          println(getMethod.getResponseBodyAsString)
          throw new Exception(("Failed to get resource, code " + status).asInstanceOf[String])
      }
    }finally{
      getMethod.releaseConnection();
    }
  }

  def uploadFile(path:String, file:File) = {
    writeData(path, new FilePart("data", new FilePartSource(file)), Map[String, String]())
  }

  def updateFile(path:String, file:File, metadata:Map[String, String]) = {

    val putMethod = new PutMethod(url + path)

    addAuthHeader(putMethod, requestUri + path)


    var partList:List[Part] = metadata.map(e => new StringPart(e._1, e._2, "UTF-8")).toList

    if(file != null){
      partList = new FilePart("data", new FilePartSource(file)) :: partList
    }

    putMethod.setRequestEntity(new MultipartRequestEntity(partList.toArray, putMethod.getParams))

    try{
      val status = httpClient.executeMethod(putMethod)
      status match {
        case HttpStatus.SC_OK =>
        case _ =>
          println(putMethod.getResponseBodyAsString)
          throw new Exception(("Filed to put resource, code " + status).asInstanceOf[String])
      }
    }finally {
      putMethod.releaseConnection
    }
  }

  private def writeData(path:String, filePart:FilePart, metadata:Map[String, String]) = {
    val postMethod = new PostMethod(url + path)

    addAuthHeader(postMethod, requestUri)

    val parts:Array[Part] = (filePart ::
            metadata.map(e => new StringPart(e._1, e._2, "UTF-8")).toList).toArray

    postMethod.setRequestEntity(new MultipartRequestEntity(parts, postMethod.getParams))

    try{
      val status = httpClient.executeMethod(postMethod)
      status match {
        case HttpStatus.SC_CREATED => {
        }
        case _ =>
          println(postMethod.getResponseBodyAsString)
          throw new Exception(("Filed to post resource, code " + status).asInstanceOf[String])
      }
    }finally {
      postMethod.releaseConnection
    }
  }

  def makeDir(path:String) = {

    val postMethod = new PostMethod(url + path)

    addAuthHeader(postMethod, requestUri + path)
    postMethod.addRequestHeader("x-c3-nodetype", "directory")

    try{
      val status = httpClient.executeMethod(postMethod)
      status match {
        case HttpStatus.SC_CREATED => {

        }
        case _ => throw new Exception(("Failed to make directory, message: " + postMethod.getResponseBodyAsString))
      }
    }
  }

  def getNodeMetadata(path:String):String = {
    val getMethod = new GetMethod(url + path + "?metadata")

    addAuthHeader(getMethod, requestUri + path)

    try{
      val status = httpClient.executeMethod(getMethod)
      status match {
        case HttpStatus.SC_OK => {
         getMethod.getResponseBodyAsString
        }
        case _ =>
          println(getMethod.getResponseBodyAsString)
          throw new Exception(("Failed to get resource, code " + status).asInstanceOf[String])
      }
    }finally{
      getMethod.releaseConnection();
    }
  }

  def getNodeMetadataAsXML(path:String):Elem = {
    val getMethod = new GetMethod(url + path + "?metadata")

    addAuthHeader(getMethod, requestUri + path)

    try{
      val status = httpClient.executeMethod(getMethod)
      status match {
        case HttpStatus.SC_OK => {
          XML.load(getMethod.getResponseBodyAsStream)
        }
        case _ =>
          println(getMethod.getResponseBodyAsString)
          throw new Exception(("Failed to get resource, code " + status).asInstanceOf[String])
      }
    }finally{
      getMethod.releaseConnection();
    }
  }

  def delete(path:String) = {
    val deleteMethod = new DeleteMethod(url + path)

    addAuthHeader(deleteMethod, requestUri + path)

    try{
      val status = httpClient.executeMethod(deleteMethod)
      status match {
        case HttpStatus.SC_OK => {

        }
        case _ =>
          println(deleteMethod.getResponseBodyAsString)
          throw new Exception(("Failed to delete file, code " + status).asInstanceOf[String])
      }
    }
  }

  def addAuthHeader(method:HttpMethodBase, resource:String) = {
    if(username != "anonymous"){

      val strToHash = username + key + resource

      val md5 = new MD5
      md5.Update(strToHash.getBytes)

      val hash = MD5.asHex(md5.Final)
      val header = new Header("x-c3-auth", username + ":" + hash)
      method.addRequestHeader(header)
    }
  }

}