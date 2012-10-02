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
import java.nio.channels.{FileChannel, Channels, WritableByteChannel}
import java.nio.file.{Path, StandardCopyOption, StandardOpenOption, Files}

object DataStream{
  
  def create(file:File):DataStream = new FileDataStream(file)
  
  def create(data:Array[Byte]):DataStream = new BytesDataStream(data)
  
  def create(value:String):DataStream = new StringDataStream(value)
  
  def createEmpty:DataStream = new EmptyDataStream
  
}

/**
 * Abstract data representation in the system
 * It aimed to incapsulate specific data format, for example byte array, string, file, input stream, BDB entry etc.
 *
 * This class is not thread-safe
 */

abstract class DataStream {

  val logger = LogFactory.getLog(DataStream.getClass)

  /**
   *
   */
  lazy val hash:String  = MD5.asHex(byteHash)

  lazy val byteHash:Array[Byte] = calculateHash

  /**
   * Obviously method returns input stream to read data
   */
  def inputStream:InputStream

  /**
   * Write content to the channel
   */
  def writeTo(channel:WritableByteChannel)

  /**
   * Write to some path
   */
  def writeTo(path:Path)

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
  protected
  def calculateHash:Array[Byte]

  /**
   * Writes content to the specified file.
   * All previous file content will be lost
   */
  def writeTo(targetFile:File) {
    val channel : WritableByteChannel = new FileOutputStream(targetFile).getChannel

    try{
      this writeTo channel
    }finally{
      channel.close()
    }
  }

  /**
   * Writes content to the specified output stream
   */
  def writeTo(out:OutputStream) {
    this.writeTo(Channels.newChannel(out))
  }

  /**
   * Get data as byte array.
   * Please note, array may be huge and method can cause OutOfMemoryException
   */
  def getBytes:Array[Byte] = {
    val stream = new ByteArrayOutputStream
    writeTo(stream)
    stream.toByteArray
  }

  def copy:DataStream

  
  protected def top(types:java.util.Collection[_]):String = {
    types.iterator.next.asInstanceOf[MimeType].toString
  }
  
}

abstract class AbstractFileDataStream extends DataStream{

  lazy val file:File = getFile

  def getFile:File

  def inputStream = new FileInputStream(file)

  def writeTo(channel:WritableByteChannel) {
    val fileChannel = new FileInputStream(file).getChannel

    try{
      fileChannel.transferTo(0, file.length, channel)
    }finally{
      fileChannel.close()
    }
  }

  def writeTo(path:Path){
    Files.copy(file.toPath, path, StandardCopyOption.REPLACE_EXISTING)
  }

  override def writeTo(targetFile:File) {

    if(file.getCanonicalFile != targetFile.getCanonicalFile)
      Files.copy(file.toPath, targetFile.toPath, StandardCopyOption.REPLACE_EXISTING)
    else{
      logger.warn("Trying to write file to itself, skipping")
    }
  }

  def stringValue:String = {
    new String(Files.readAllBytes(file.toPath), "UTF-8")
  }

  protected
  def calculateHash = MD5.getHash(file)

  def length:Long = file.length

  def mimeType:String = top(MimeUtil.getMimeTypes(file))

}

/**
 * DataStream implementation that incapsulates file
 */
class FileDataStream(val existentFile:File) extends AbstractFileDataStream{

  override def getFile:File = existentFile

  def copy:DataStream = new FileDataStream(existentFile)
  
}

/**
 * Abstract implementation of DataStream that wraps byte array
 * The way to obtain this byte array must be defined in #{AbstractBytesDataStream.loadBytes} method
 */
abstract class AbstractBytesDataStream extends DataStream {

  protected def loadBytes:Array[Byte]

  def inputStream = new ByteArrayInputStream(loadBytes)

  def writeTo(channel:WritableByteChannel) {
    channel.write(ByteBuffer.wrap(loadBytes))
  }

  override def writeTo(targetFile:File) {
    Files.write(targetFile.toPath, loadBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
  }

  def writeTo(path:Path){
    Files.write(path, loadBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
  }

  override def getBytes:Array[Byte] = loadBytes

  protected
  def calculateHash = {
    val md5 = new MD5
    md5.Update(loadBytes)
    md5.Final
  }

  def stringValue:String = new String(loadBytes)

  def length:Long = loadBytes.length

  def mimeType:String = top(MimeUtil.getMimeTypes(loadBytes))
}

/**
 * AbstractBytesDataStream implementation that simply use byte array passed to constructor
 */
class BytesDataStream(val bytes:Array[Byte]) extends AbstractBytesDataStream {

  override def loadBytes:Array[Byte] = bytes

  def copy:DataStream = new BytesDataStream(bytes)

}

/**
 * ByteWrapper implementation that wraps string
 */
class StringDataStream(val value:String) extends DataStream {
  
  def inputStream = new ByteArrayInputStream(value.getBytes("UTF-8"))
  
  def writeTo(channel:WritableByteChannel) {
    channel.write(ByteBuffer.wrap(value.getBytes("UTF-8")))
  }

  def writeTo(path:Path){
    Files.write(path, value.getBytes("UTF-8"), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
  }
  
  def stringValue:String = value
  
  def length:Long = value.getBytes("UTF-8").length

  protected
  def calculateHash = {
    val md5 = new MD5
    md5.Update(value.getBytes("UTF-8"))
    md5.Final
  }
  
  def mimeType:String = top(MimeUtil.getMimeTypes(value))

  def copy:DataStream = new StringDataStream(value)

}


/**
 * DataStream that presents empty data. It has zero length, and empty mime-type
 */
class EmptyDataStream extends DataStream {
  def inputStream = new ByteArrayInputStream(new Array[Byte](0))
  
  def writeTo(channel:WritableByteChannel) {
    channel.write(ByteBuffer.wrap(new Array[Byte](0)))
  }

  def writeTo(path:Path){
    Files.write(path, new Array[Byte](0), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
  }
  
  def stringValue:String = ""
  
  def length:Long = 0

  protected
  def calculateHash = {
    val md5 = new MD5
    md5.Update("".getBytes)
    md5.Final
  }

  def mimeType:String = ""

  def copy:DataStream = new EmptyDataStream
}

class PathDataStream(val path:Path) extends AbstractFileDataStream{

  var tmpFileCreated = false

  private var tmpFile:File = null

  def getFile:File = {
    if (tmpFile == null){
      tmpFile = Files.createTempFile("Pathds", "" + System.currentTimeMillis()).toFile
      tmpFileCreated = true
    }
    tmpFile
  }
  /**
   * Write to some path
   */
  override
  def writeTo(targetPath: Path) {
    Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING)
  }

  override
  def getBytes:Array[Byte] = {
    Files.readAllBytes(path)
  }

  /**
   * Data length in bytes
   */
  override
  def length = {
    Files.size(path)
  }

  override def finalize(){
    super.finalize()
    if (tmpFileCreated){
      try{
        Files.delete(tmpFile.toPath)
      }catch {
        case e:Throwable =>
      }
    }
  }

  def copy = throw new RuntimeException("Operaion is not defined")
}