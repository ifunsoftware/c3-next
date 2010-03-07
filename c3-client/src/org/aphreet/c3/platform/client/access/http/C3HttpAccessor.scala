package org.aphreet.c3.platform.client.access.http

import java.io.{ByteArrayInputStream, InputStream}
import org.apache.commons.httpclient.methods.multipart._
import org.apache.commons.httpclient.{HttpStatus, HttpClient}
import org.apache.commons.httpclient.methods.{GetMethod, PostMethod}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 6, 2010
 * Time: 11:11:35 PM
 * To change this template use File | Settings | File Templates.
 */

class C3HttpAccessor(val url:String){

  val httpClient = new HttpClient

  def write(data:Array[Byte], metadata:Map[String, String]):String =  {
    val postMethod = new PostMethod(url)

    val parts:Array[Part] = (new FilePart("data", new ByteArrayPartSource(data)) ::
            metadata.map(e => new StringPart(e._1, e._2)).toList).toArray

    postMethod.setRequestEntity(new MultipartRequestEntity(parts, postMethod.getParams()));

    try{
      httpClient.executeMethod(postMethod) match {
        case HttpStatus.SC_OK => postMethod.getResponseBodyAsString().replaceAll("\n", "")
        case _ => throw new Exception(("Failed to get file, code " + _).asInstanceOf[String])
      }
    }finally{
      postMethod.releaseConnection()
    }
  }

  def read(address:String):Array[Byte] = {
    val getMethod = new GetMethod(url + address)

    try{
      return httpClient.executeMethod(getMethod) match {
        case HttpStatus.SC_OK => getMethod.getResponseBody
        case _ => throw new Exception(("Failed to get file, code " + _).asInstanceOf[String])
      }
    }finally{
      getMethod.releaseConnection();
    }
  }

  def fakeRead(address:String):Int = {
    val getMethod = new GetMethod(url + address)

    try{
      return httpClient.executeMethod(getMethod) match {
        case HttpStatus.SC_OK => {
          val stream = getMethod.getResponseBodyAsStream
          var read = 0
          while(stream.read != -1){read = read + 1}
          read
        }
        case _ => throw new Exception(("Failed to get file, code " + _).asInstanceOf[String])
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