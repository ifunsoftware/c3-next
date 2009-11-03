package org.aphreet.c3.platform.resource

import scala.collection.jcl.{HashMap, Map, Buffer, ArrayList}

import java.util.{Map => JMap}

import java.util.Date

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

class Resource {
	
  var address:String = null
  
  var createDate:Date = new Date
  
  var metadata:HashMap[String, String] = new HashMap
 
  var systemMetadata:HashMap[String, String] = new HashMap
  
  var versions:Buffer[ResourceVersion] = new ArrayList
  
  var data:DataWrapper = null
  
  
  def getMetadata:JMap[String, String] = metadata.underlying;
  
  def getSysMetadata:JMap[String, String] = metadata.underlying;
  
  
  
  override def toString:String = {
    address + " " + createDate + " " + metadata + " " + systemMetadata + " " + versions
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
  val MD_EMBEDDED_CONTENT = "c3.embedded.content"
  
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
        
        result + version
      }
      
      result
    }
    
    
    val resource = new Resource
    
    val byteIn = new ByteArrayInputStream(bytes)
    val dataIn = new DataInputStream(byteIn)
    
    dataIn.readInt //resource class version
    
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
