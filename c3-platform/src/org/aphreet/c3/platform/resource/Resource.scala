package org.aphreet.c3.platform.resource

import scala.collection.jcl.{HashMap, Map, Buffer, ArrayList}

import java.util.{Map => JMap, List => JList}

import java.util.Date

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

class Resource {
	
  var address:String = null
  
  var createDate:Date = new Date
  
  var metadata:HashMap[String, String] = new HashMap
 
  var systemMetadata:HashMap[String, String] = new HashMap
  
  var versions:ArrayList[ResourceVersion] = new ArrayList
  
  var isVersioned = false;
  
  
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
  
  def getMetadata:JMap[String, String] = metadata.underlying
  
  def getSysMetadata:JMap[String, String] = systemMetadata.underlying
  
  def getVersions:JList[ResourceVersion] = versions.underlying
  
  def addVersion(version:ResourceVersion){
    if(!isVersioned){
      versions.clear
    }
    versions add version
  }
  
  override def toString:String = {
    
    val builder = new StringBuilder
    
    builder.append("Resource:\n").append("Address: ").append(address)
    	.append("\nCreate date: ").append(createDate)
    	.append("\n\nMetadata: ")
    
    for((key, value) <- metadata){
      builder.append("\n").append(key).append(" -> ").append(value)
    }
    
    builder.append("\n\nSystem metadata:" )
    
    for((key, value) <- systemMetadata){
      builder.append("\n").append(key).append(" -> ").append(value)
    }
    
    builder.append("\n\nVersions:")
    
    for(i <- 1 to versions.size){
      builder.append("\nVer ").append(i).append(": ").append(versions(i-1).toString)
    }
    
    builder.toString
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
 
    dataOs.writeInt(0) //resource class verison, for future
    
    dataOs.writeBoolean(isVersioned)
    
    writeString(address, dataOs)
    writeDate(createDate, dataOs)
    
    
	writeMap(metadata, dataOs)
	writeMap(systemMetadata, dataOs)
 
	writeVersions(versions, dataOs)
 
	dataOs.writeLong(Resource.STOP_SEQ)
 
	byteOs.toByteArray
  }

}

object Resource {
  
  val STOP_SEQ : Long = 107533894376158093L
  
  val MD_CONTENT_TYPE = "content.type"
  val MD_DATA_ADDRESS = "c3.data.address"
  val MD_CONTENT_TYPE_DEFAULT = "application/octet-stream"
  
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
    val dataIn = new DataInputStream(byteIn)
    
    dataIn.readInt //resource class version
    
    resource.isVersioned = dataIn.readBoolean
    
    resource.address = readString(dataIn)
    resource.createDate = new Date(dataIn.readLong)
    
    resource.metadata ++= readMap(dataIn)
    resource.systemMetadata ++= readMap(dataIn)
    
    resource.versions ++= readVersions(dataIn)
    
    val stopSeq = dataIn.readLong
    if(stopSeq != STOP_SEQ){
      throw new ResourceException("Failed to deserialize resource")
    }
    
    dataIn.close
    
    resource
  }
}
