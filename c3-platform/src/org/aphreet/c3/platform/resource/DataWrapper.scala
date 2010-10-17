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

package org.aphreet.c3.platform.resource

import java.nio.ByteBuffer

import org.apache.commons.logging.LogFactory

import eu.medsea.mimeutil.{MimeUtil, MimeType}
import java.io._
import com.twmacinta.util.MD5
import java.nio.channels.{Channels, WritableByteChannel}

object DataWrapper{
  
  def wrap(file:File) = new FileDataWrapper(file)
  
  def wrap(data:Array[Byte]) = new BytesDataWrapper(data)
  
  def wrap(value:String) = new StringDataWrapper(value)
  
  def empty = new EmptyDataWrapper
  
}

/**
 * Abstract data representation in the system
 * It aimed to incapsulate specific data format, for example byte array, string, file, input stream, BDB entry etc.
 *
 * This class is not thread-safe
 */

abstract class DataWrapper {

  val logger = LogFactory.getLog(DataWrapper.getClass)

  /**
   *
   */
  lazy val hash:String  = calculateHash

  /**
   * Obviously method returns input stream to read data
   */
  def inputStream:InputStream

  /**
   * Write content to the channel
   */
  def writeTo(channel:WritableByteChannel):Unit

  /**
   * Get content as a string. As far as we can have binary data, string may not be readable
   */
  def stringValue:String

  /**
   * Data length in bytes
   */
  def length:Long

  /**
   * Mime-type of the data
   */
  def mimeType:String

  /**
   * Method used to calculate data hash
   * It possibly should be protected, but anyway, please use #{hash} value instead
   */
  //TODO check if this method can be protected
  def calculateHash:String

  /**
   * Writes content to the specified file.
   * All previous file content will be lost
   */
  def writeTo(targetFile:File):Unit = {
    val channel : WritableByteChannel = new FileOutputStream(targetFile).getChannel()

    try{
      this writeTo channel
    }finally{
      channel.close
    }
  }

  /**
   * Writes content to the specified output stream
   */
  def writeTo(out:OutputStream):Unit = this.writeTo(Channels.newChannel(out))

  /**
   * Get data as byte array.
   * Please note, array may be huge and method can cause OutOfMemoryException
   */
  def getBytes:Array[Byte] = {
    val stream = new ByteArrayOutputStream;
    writeTo(stream)
    stream.toByteArray
  }
  

  
  protected def top(types:java.util.Collection[_]):String = {
    types.iterator.next.asInstanceOf[MimeType].toString
  }
  
}

/**
 * DataWrapper implementation that incapsulates file
 */
class FileDataWrapper(val file:File) extends DataWrapper{

  def inputStream = new FileInputStream(file)
  
  def writeTo(channel:WritableByteChannel) = {
    val fileChannel = new FileInputStream(file).getChannel
    
    try{
      fileChannel.transferTo(0, file.length, channel)
    }finally{
      fileChannel.close
    }
  }

  override def writeTo(targetFile:File) = {

    if(file.getCanonicalFile != targetFile.getCanonicalFile)
       super.writeTo(targetFile)
    else{
      logger.warn("Trying to write file to itself, skipping")
    }
  }
  
  def stringValue:String = {
    val out = new ByteArrayOutputStream
    
    this writeTo out

    new String(out.toByteArray)
  }

  def calculateHash:String = MD5.asHex(MD5.getHash(file))

  def length:Long = file.length

  def mimeType:String = top(MimeUtil.getMimeTypes(file))
  
}

/**
 * Abstract implementation of DataWrapper that wraps byte array
 * The way to obtain this byte array must be defined in #{AbstractBytesDataWrapper.loadBytes} method
 */
abstract class AbstractBytesDataWrapper extends DataWrapper {

  protected def loadBytes:Array[Byte]

  def inputStream = new ByteArrayInputStream(loadBytes)

  def writeTo(channel:WritableByteChannel) = channel.write(ByteBuffer.wrap(loadBytes))

  override def getBytes:Array[Byte] = loadBytes

  def calculateHash:String = MD5.asHex({
    val md5 = new MD5
    md5.Update(loadBytes)
    md5.Final
  })

  def stringValue:String = new String(loadBytes)

  def length:Long = loadBytes.length

  def mimeType:String = top(MimeUtil.getMimeTypes(loadBytes))


}

/**
 * AbstractBytesDataWrapper implementation that simply use byte array passed to constructor
 */
class BytesDataWrapper(val bytes:Array[Byte]) extends AbstractBytesDataWrapper {

  override def loadBytes:Array[Byte] = bytes

}

/**
 * ByteWrapper implementation that wraps string
 */
class StringDataWrapper(val value:String) extends DataWrapper {
  
  def inputStream = new ByteArrayInputStream(value.getBytes("UTF-8"))
  
  def writeTo(channel:WritableByteChannel) = channel.write(ByteBuffer.wrap(value.getBytes("UTF-8")))
  
  def stringValue:String = value
  
  def length:Long = value.getBytes("UTF-8").length

  def calculateHash:String = MD5.asHex({
    val md5 = new MD5
    md5.Update(value.getBytes("UTF-8"))
    md5.Final
  })
  
  def mimeType:String = top(MimeUtil.getMimeTypes(value))
}


/**
 * DataWrapper that presents empty data. It has zero length, and empty mime-type
 */
class EmptyDataWrapper extends DataWrapper {
  def inputStream = new ByteArrayInputStream(new Array[Byte](0))
  
  def writeTo(channel:WritableByteChannel) = channel.write(ByteBuffer.wrap(new Array[Byte](0)))
  
  def stringValue:String = ""
  
  def length:Long = 0

  def calculateHash:String = MD5.asHex({
    val md5 = new MD5
    md5.Update("".getBytes())
    md5.Final
  })

  def mimeType:String = ""
}
