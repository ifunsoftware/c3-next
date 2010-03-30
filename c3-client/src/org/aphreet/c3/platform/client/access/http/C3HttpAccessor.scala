package org.aphreet.c3.platform.client.access.http

import org.apache.commons.httpclient.methods.multipart._
import org.apache.commons.httpclient.{HttpStatus, HttpClient}
import org.apache.commons.httpclient.methods.{DeleteMethod, GetMethod, PostMethod}
import java.io.{File, ByteArrayInputStream, InputStream}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 6, 2010
 * Time: 11:11:35 PM
 * To change this template use File | Settings | File Templates.
 */

class C3HttpAccessor(val url:String){

  val httpClient = new HttpClient

  def write(data:Array[Byte], metadata:Map[String, String]):String =
    writeData(new FilePart("data", new ByteArrayPartSource(data)), metadata)

  def upload(file:File, metadata:Map[String, String]):String =
    writeData(new FilePart("data", new FilePartSource(file)), metadata)

  private def writeData(filePart:FilePart, metadata:Map[String, String]):String = {
    val postMethod = new PostMethod(url)

    val parts:Array[Part] = (filePart ::
            metadata.map(e => new StringPart(e._1, e._2)).toList).toArray

    postMethod.setRequestEntity(new MultipartRequestEntity(parts, postMethod.getParams))

    try{
      val status = httpClient.executeMethod(postMethod)
      status match {
        case HttpStatus.SC_OK => postMethod.getResponseBodyAsString().replaceAll("\n", "")
        case _ => throw new Exception(("Filed to post resource, code" + status).asInstanceOf[String])
      }
    }finally {
      postMethod.releaseConnection
    }
  }

  def read(address:String):Array[Byte] = {
    val getMethod = new GetMethod(url + address)

    try{
      val status = httpClient.executeMethod(getMethod)
      return status match {
        case HttpStatus.SC_OK => getMethod.getResponseBody
        case _ => throw new Exception(("Failed to get resource, code " + status).asInstanceOf[String])
      }
    }finally{
      getMethod.releaseConnection();
    }
  }

  def delete(address:String) = {
    val deleteMethod = new DeleteMethod(url + address)

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

    try{
      val status = httpClient.executeMethod(getMethod)
      return status match {
        case HttpStatus.SC_OK => {
          val stream = getMethod.getResponseBodyAsStream
          var read = 0
          while(stream.read != -1){read = read + 1}
          read
        }
        case _ => throw new Exception(("Failed to get resource, code " + status).asInstanceOf[String])
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