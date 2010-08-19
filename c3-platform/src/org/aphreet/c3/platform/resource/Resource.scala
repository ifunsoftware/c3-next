package org.aphreet.c3.platform.resource

import scala.collection.mutable.{HashMap, Map, Buffer, ArrayBuffer}

import java.util.Date

import com.twmacinta.util.{MD5, MD5InputStream}
import java.io._
import org.aphreet.c3.platform.common.JSONFormatter
import com.springsource.json.writer.JSONWriterImpl

class Resource {

  var address:String = null

  var createDate = new Date

  var metadata = new HashMap[String, String]

  var systemMetadata = new HashMap[String, String]

  var versions = new ArrayBuffer[ResourceVersion]

  var isVersioned = false

  def mimeType:String = {
    if(metadata != null){
      metadata.get(Resource.MD_CONTENT_TYPE) match{
        case Some(t) => return t
        case None => null
      }
    }

    if(versions.size > 0){
      val data = versions(0).data
      if(data != null) return data.mimeType
    }

    return Resource.MD_CONTENT_TYPE_DEFAULT
  }

  def calculateCheckSums = {
    if(!this.isVersioned){
      versions(0).calculateHash
    }else{
      versions.filter(!_.persisted).foreach(_.calculateHash)
    }
  }

  def addVersion(version:ResourceVersion){
    if(!isVersioned){
      versions.clear
    }
    versions += version
  }

  def toByteArray:Array[Byte] = {

    def writeDate(value:Date, dataOs:DataOutputStream) = {
      var date = new Date(0)
      if(value != null) date = value
      dataOs.writeLong(date.getTime)
    }

    def writeString(value:String, dataOs:DataOutputStream) = {

      var string:String = ""

      if(value != null) string = value

      val bytes = string.getBytes(Resource.MD_ENCODING)
      dataOs.writeInt(bytes.length)
      dataOs.write(bytes)
    }

    def writeMap(map:Map[String, String], dataOs:DataOutputStream) = {
      dataOs.writeInt(map.size)

      for(key <- map.keySet){
        writeString(key, dataOs)
        writeString(map(key), dataOs)
      }
    }

    def writeVersions(versions:Buffer[ResourceVersion], dataOs:DataOutputStream) = {
      dataOs.writeInt(0) //version class version, for future
      dataOs.writeInt(versions.size)

      for(version <- versions){
        dataOs.writeInt(version.revision)
        writeDate(version.date, dataOs)
        writeMap(version.systemMetadata, dataOs)
      }
    }


    val byteOs = new ByteArrayOutputStream
    val dataOs = new DataOutputStream(byteOs)

    dataOs.writeInt(2) //resource class version, for future

    dataOs.writeBoolean(isVersioned)

    writeString(address, dataOs)
    writeDate(createDate, dataOs)


    writeMap(metadata, dataOs)
    writeMap(systemMetadata, dataOs)

    writeVersions(versions, dataOs)

    val md5 = new MD5
    md5.Update(byteOs.toByteArray)

    byteOs.write(md5.Final)

    byteOs.toByteArray
  }

}

object Resource {

  val STOP_SEQ : Long = 107533894376158093L

  val MD_CONTENT_TYPE = "content.type"
  val MD_DATA_ADDRESS = "c3.data.address"
  val MD_CONTENT_TYPE_DEFAULT = "application/octet-stream"
  val MD_POOL = "c3.pool"
  val MD_TAGS = "c3.tags"
  val MD_USER = "c3.user"
  val MD_ENCODING = "UTF-8"
  
  def fromByteArray(bytes:Array[Byte]):Resource = {

    def readString(dataIs:DataInputStream, version:Int):String = {
      val strSize = dataIs.readInt
      val strArray = new Array[Byte](strSize)
      dataIs.read(strArray)
      if(version <= 1){
        new String(strArray)
      }else{
        new String(strArray, MD_ENCODING)
      }

    }

    def readDate(dataIs:DataInputStream):Date = {
      new Date(dataIs.readLong)
    }

    def readMap(dataIs:DataInputStream, version:Int):HashMap[String, String] = {
      val map = new HashMap[String, String]

      val mapSize = dataIs.readInt

      for(i <- 1 to mapSize){
        val key = readString(dataIs, version)
        val value = readString(dataIs, version)
        map.put(key, value)
      }
      map
    }

    def readVersions(dataIs:DataInputStream, serializeVersion:Int):Buffer[ResourceVersion] = {

      val result = new ArrayBuffer[ResourceVersion]

      dataIs.readInt //read version
      val count = dataIs.readInt

      for(i <- 1 to count){
        val version = new ResourceVersion
        version.revision = dataIs.readInt
        version.date = new Date(dataIs.readLong)
        version.systemMetadata = readMap(dataIs, serializeVersion)
        version.persisted = true

        result += version
      }

      result
    }


    val resource = new Resource

    val byteIn = new ByteArrayInputStream(bytes)

    val md5Is = new MD5InputStream(byteIn)

    val dataIn = new DataInputStream(md5Is)

    val serializeVersion = dataIn.readInt //resource class version

    resource.isVersioned = dataIn.readBoolean

    resource.address = readString(dataIn, serializeVersion)
    resource.createDate = new Date(dataIn.readLong)

    resource.metadata ++= readMap(dataIn, serializeVersion)
    resource.systemMetadata ++= readMap(dataIn, serializeVersion)

    resource.versions ++= readVersions(dataIn, serializeVersion)

    serializeVersion match{
      case 0 => {
        val stopSeq = dataIn.readLong
        if(stopSeq != STOP_SEQ){
          throw new ResourceException("Failed to deserialize resource, wrong stop sequince")
        }
      }
      case _ => {
        val currentSumm = md5Is.getMD5.Final
        val savedSumm = new Array[Byte](16)

        dataIn.read(savedSumm)

        if(currentSumm.equals(savedSumm)){
          throw new ResourceException("Failed to deserialize resource, md5 hashes do not match")
        }
      }
    }



    dataIn.close

    resource
  }
}
