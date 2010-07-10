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

abstract class DataWrapper {

  val logger = LogFactory.getLog(DataWrapper.getClass)

  lazy val hash:String  = calculateHash

  def inputStream:InputStream

  def writeTo(channel:WritableByteChannel):Unit

  def stringValue:String

  def length:Long

  def mimeType:String

  def calculateHash:String

  def writeTo(targetFile:File):Unit = {
    val channel : WritableByteChannel = new FileOutputStream(targetFile).getChannel()

    try{
      this writeTo channel
    }finally{
      channel.close
    }
  }
  
  def writeTo(out:OutputStream):Unit = this.writeTo(Channels.newChannel(out))
  
  def getBytes:Array[Byte] = {
    val stream = new ByteArrayOutputStream;
    writeTo(stream)
    stream.toByteArray
  }
  

  
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
  
  def stringValue:String = {
    val out = new ByteArrayOutputStream
    
    this writeTo out

    new String(out.toByteArray)
  }

  def calculateHash:String = MD5.asHex(MD5.getHash(file))

  def length:Long = file.length

  def mimeType:String = top(MimeUtil.getMimeTypes(file))
  
}

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

class BytesDataWrapper(val bytes:Array[Byte]) extends AbstractBytesDataWrapper {

  override def loadBytes:Array[Byte] = bytes

}


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
