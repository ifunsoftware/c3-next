package org.aphreet.c3.platform.resource

import scala.collection.jcl.{HashMap, Map, Buffer, ArrayList}

import java.util.{Map => JMap, List => JList}

import java.util.Date

import com.twmacinta.util.{MD5, MD5InputStream}
import java.io._
import org.aphreet.c3.platform.common.JSONFormatter
import com.springsource.json.writer.JSONWriterImpl

class Resource {

  var address:String = null

  var createDate:Date = new Date

  var metadata:HashMap[String, String] = new HashMap

  var systemMetadata:HashMap[String, String] = new HashMap

  var versions:ArrayList[ResourceVersion] = new ArrayList

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

  def getMetadata:JMap[String, String] = metadata.underlying

  def getSysMetadata:JMap[String, String] = systemMetadata.underlying

  def getVersions:JList[ResourceVersion] = versions.underlying

  def addVersion(version:ResourceVersion){
    if(!isVersioned){
      versions.clear
    }
    versions add version
  }

  override def toString:String = toJSON(true)

  def toByteArray:Array[Byte] = {

    def writeDate(value:Date, dataOs:DataOutputStream) = {
      var date = new Date(0)
      if(value != null) date = value
      dataOs.writeLong(date.getTime)
    }

    def writeString(value:String, dataOs:DataOutputStream) = {

      var string:String = ""

      if(value != null) string = value

      val bytes = string.getBytes
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

    dataOs.writeInt(1) //resource class version, for future

    dataOs.writeBoolean(isVersioned)

    writeString(address, dataOs)
    writeDate(createDate, dataOs)


    writeMap(metadata, dataOs)
    writeMap(systemMetadata, dataOs)

    writeVersions(versions, dataOs)

    //dataOs.writeLong(Resource.STOP_SEQ)

    val md5 = new MD5
    md5.Update(byteOs.toByteArray)

    byteOs.write(md5.Final)

    byteOs.toByteArray
  }

  def toJSON(full:Boolean):String = {

    val swriter = new StringWriter()


    try{
      val writer = new JSONWriterImpl(swriter)

      writer.`object`

      writer.key("address").value(this.address)

      writer.key("createDate").value(this.createDate.getTime)


      writer.key("metadata")

      writer.`object`

      metadata.foreach((e:(String, String)) => writer.key(e._1).value(e._2))

      writer.endObject

      if(full){
        writer.key("systemMetadata")

        writer.`object`

        systemMetadata.foreach((e:(String, String)) => writer.key(e._1).value(e._2))

        writer.endObject
      }

      writer.key("versions")

      writer.array

      versions.foreach(v => {
        writer.`object`

        writer.key("createDate").value(v.date.getTime)
        writer.key("dataLength").value(v.data.length)


        if(full){
          writer.key("revision").value(v.revision)

          writer.key("systemMetadata")

          writer.`object`

          v.systemMetadata.foreach((e:(String, String)) => writer.key(e._1).value(e._2))
          
          writer.endObject
        }
        writer.endObject
      })

      writer.endArray


      writer.endObject
      swriter.flush

      JSONFormatter.format(swriter.toString)

    }finally{
      swriter.close
    }
  }

}

object Resource {

  val STOP_SEQ : Long = 107533894376158093L

  val MD_CONTENT_TYPE = "content.type"
  val MD_DATA_ADDRESS = "c3.data.address"
  val MD_CONTENT_TYPE_DEFAULT = "application/octet-stream"
  val MD_POOL = "c3.pool"
  val MD_TAGS = "c3.tags"
  
  def fromByteArray(bytes:Array[Byte]):Resource = {

    def readString(dataIs:DataInputStream):String = {
      val strSize = dataIs.readInt
      val strArray = new Array[Byte](strSize)
      dataIs.read(strArray)
      new String(strArray)
    }

    def readDate(dataIs:DataInputStream):Date = {
      new Date(dataIs.readLong)
    }

    def readMap(dataIs:DataInputStream):HashMap[String, String] = {
      val map = new HashMap[String, String]

      val mapSize = dataIs.readInt

      for(i <- 1 to mapSize){
        val key = readString(dataIs)
        val value = readString(dataIs)
        map.put(key, value)
      }
      map
    }

    def readVersions(dataIs:DataInputStream):Buffer[ResourceVersion] = {

      val result = new ArrayList[ResourceVersion]

      dataIs.readInt //read version
      val count = dataIs.readInt

      for(i <- 1 to count){
        val version = new ResourceVersion
        version.revision = dataIs.readInt
        version.date = new Date(dataIs.readLong)
        version.systemMetadata = readMap(dataIs)
        version.persisted = true

        result + version
      }

      result
    }


    val resource = new Resource

    val byteIn = new ByteArrayInputStream(bytes)

    val md5Is = new MD5InputStream(byteIn)

    val dataIn = new DataInputStream(md5Is)

    val serializeVersion = dataIn.readInt //resource class version

    resource.isVersioned = dataIn.readBoolean

    resource.address = readString(dataIn)
    resource.createDate = new Date(dataIn.readLong)

    resource.metadata ++= readMap(dataIn)
    resource.systemMetadata ++= readMap(dataIn)

    resource.versions ++= readVersions(dataIn)

    serializeVersion match{
      case 0 => {
        val stopSeq = dataIn.readLong
        if(stopSeq != STOP_SEQ){
          throw new ResourceException("Failed to deserialize resource, wrong stop sequince")
        }
      }
      case 1 => {
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
