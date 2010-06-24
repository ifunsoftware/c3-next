package org.aphreet.c3.platform.client.access.http

import org.apache.commons.httpclient.methods.multipart._
import org.apache.commons.httpclient.{HttpStatus, HttpClient}
import org.apache.commons.httpclient.methods.{DeleteMethod, GetMethod, PostMethod}
import java.nio.channels.Channels
import java.io._
import java.nio.ByteBuffer

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
        case HttpStatus.SC_OK => postMethod.getResponseBodyAsString().replaceAll("\n", "")
        case _ => throw new Exception(("Filed to post resource, code " + status).asInstanceOf[String])
      }
    }finally {
      postMethod.releaseConnection
    }
  }

  def downloadData(address:String, file:File) = {
    val getMethod = new GetMethod(url + address + "/data")

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
    val getMethod = new GetMethod(url + address + "/metadata")

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

    try{
      val status = httpClient.executeMethod(deleteMethod)
      status match{
        case HttpStatus.SC_OK => null
        case _ => throw new Exception(("Failed to delete resource, code " + status).asInstanceOf[String])
      }
    }
  }

  def fakeRead(address:String):Int = {
    val getMethod = new GetMethod(url + address + "/data")

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