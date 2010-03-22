package org.aphreet.c3.platform.resource

import java.nio.channels.{WritableByteChannel}
import java.nio.ByteBuffer

import org.apache.commons.logging.LogFactory

import eu.medsea.mimeutil.{MimeUtil, MimeType}
import java.io._
import com.twmacinta.util.MD5

object DataWrapper{
  
  def wrap(file:File) = new FileDataWrapper(file)
  
  def wrap(data:Array[Byte]) = new BytesDataWrapper(data)
  
  def wrap(value:String) = new StringDataWrapper(value)
  
  def empty = new EmptyDataWrapper
  
}

abstract class DataWrapper {

  val logger = LogFactory.getLog(DataWrapper.getClass)
  
  def inputStream:InputStream

  def writeTo(targetFile:File):Unit = {
    val channel : WritableByteChannel = new FileOutputStream(targetFile).getChannel()

    try{
      this writeTo channel
    }finally{
      channel.close
    }
  }
  
  def writeTo(channel:WritableByteChannel):Unit
  
  def writeTo(out:OutputStream):Unit
  
  def getBytes:Array[Byte] = {
    val stream = new ByteArrayOutputStream;
    writeTo(stream)
    stream.toByteArray
  }
  
  def stringValue:String
 
  def length:Long
  
  def mimeType:String

  def hash:String
  
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

  override def writeTo(targetFile:File) = {

    if(file.getCanonicalFile != targetFile.getCanonicalFile)
       super.writeTo(targetFile)
    else{
      logger.warn("Trying to write file to itself, skipping")
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

  def hash:String = MD5.asHex(MD5.getHash(file))

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

  def hash:String = MD5.asHex({
    val md5 = new MD5
    md5.Update(bytes)
    md5.Final
  })

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

  def hash:String = MD5.asHex({
    val md5 = new MD5
    md5.Update(value.getBytes())
    md5.Final
  })
  
  def mimeType:String = top(MimeUtil.getMimeTypes(value))
}

class EmptyDataWrapper extends DataWrapper {
  def inputStream = new ByteArrayInputStream(new Array[Byte](0))
  
  def writeTo(channel:WritableByteChannel) = channel.write(ByteBuffer.wrap(new Array[Byte](0)))
 
  def writeTo(out:OutputStream) = {}
  
  def stringValue:String = ""
  
  def length:Long = 0

  def hash:String = MD5.asHex({
    val md5 = new MD5
    md5.Update("".getBytes())
    md5.Final
  })

  def mimeType:String = ""
}
