package org.aphreet.c3.platform.resource

import java.io.{InputStream, OutputStream, File, FileInputStream, ByteArrayInputStream, ByteArrayOutputStream, BufferedInputStream, BufferedOutputStream}
import java.nio.channels.{FileChannel, WritableByteChannel}
import java.nio.ByteBuffer
import java.lang.StringBuilder

import eu.medsea.mimeutil.{MimeUtil, MimeType}

object DataWrapper{
  
  def wrap(file:File) = new FileDataWrapper(file)
  
  def wrap(data:Array[Byte]) = new BytesDataWrapper(data)
  
  def wrap(value:String) = new StringDataWrapper(value)
  
  def empty = new EmptyDataWrapper
  
}

abstract class DataWrapper {

  def inputStream:InputStream
  
  def writeTo(channel:WritableByteChannel)
  
  def writeTo(out:OutputStream)
  
  def getBytes:Array[Byte] = {
    val stream = new ByteArrayOutputStream;
    writeTo(stream)
    stream.toByteArray
  }
  
  def stringValue:String
 
  def length:Long
  
  def mimeType:String
  
  protected def top(types:java.util.Collection[_]):String = {
    types.iterator.next.asInstanceOf[MimeType].toString
  }
}


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
  
  def writeTo(out:OutputStream) = {
    val is = inputStream
    
    val bis = new BufferedInputStream(is)
    
    val buffer = new Array[Byte](2048)
    
    try{
    
      var read:Int = bis.read(buffer)
      while(read >=0){
        out.write(buffer, 0, read)
        read = bis.read(buffer)
      }
    }finally{
      bis.close()
    }
  }
  
  def stringValue:String = {
    val out = new ByteArrayOutputStream
    
    this writeTo out

    new String(out.toByteArray)
  }
  
  def length:Long = file.length

  def mimeType:String = top(MimeUtil.getMimeTypes(file))
  
}

class BytesDataWrapper(val bytes:Array[Byte]) extends DataWrapper {
  
  def inputStream = new ByteArrayInputStream(bytes)
  
  def writeTo(channel:WritableByteChannel) = channel.write(ByteBuffer.wrap(bytes))
  
  def writeTo(out:OutputStream) = {
    out.write(bytes)
  }
  
  override def getBytes:Array[Byte] = bytes
  
  def stringValue:String = new String(bytes)
  
  def length:Long = bytes.length
  
  def mimeType:String = top(MimeUtil.getMimeTypes(bytes))
  
}


class StringDataWrapper(val value:String) extends DataWrapper {
  
  def inputStream = new ByteArrayInputStream(value.getBytes)
  
  def writeTo(channel:WritableByteChannel) = channel.write(ByteBuffer.wrap(value.getBytes))
 
  def writeTo(out:OutputStream) = {
    out.write(value.getBytes)
  }
  
  def stringValue:String = value
  
  def length:Long = value.getBytes.length
  
  def mimeType:String = top(MimeUtil.getMimeTypes(value))
}

class EmptyDataWrapper extends DataWrapper {
  def inputStream = new ByteArrayInputStream(new Array[Byte](0))
  
  def writeTo(channel:WritableByteChannel) = channel.write(ByteBuffer.wrap(new Array[Byte](0)))
 
  def writeTo(out:OutputStream) = {}
  
  def stringValue:String = ""
  
  def length:Long = 0
  
  def mimeType:String = ""
}
