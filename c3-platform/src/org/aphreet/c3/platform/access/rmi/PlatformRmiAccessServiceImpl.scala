package org.aphreet.c3.platform.access.rmi

import java.util.HashMap

import java.io.File

import scala.collection.jcl.{HashMap => JMap}

import org.aphreet.c3.platform.resource.{DataWrapper, Resource, ResourceVersion}

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("platformRmiAccessService")
class PlatformRmiAccessServiceImpl extends PlatformRmiAccessService{

  var accessEndpoint:PlatformAccessEndpoint = null
  
  @Autowired
  def setAccessEndpoint(endpoint:PlatformAccessEndpoint) = {accessEndpoint= endpoint}
 
  def add(metadata:HashMap[String, String], file:String):String = {
    
    val resource = new Resource
    resource.metadata ++ new JMap(metadata)
    
    val version = new ResourceVersion
    version.data = DataWrapper.wrap(new File(file))
    resource addVersion version
    
    accessEndpoint.add(resource)
  }
  
  def getResourceAsString(ra:String):String = {
    
    val resource = accessEndpoint get ra;
    
    if(resource != null){
      val result = resource.toString
      result
    }else{
      null
    }
  }
  
  def get(ra:String):Array[Byte] = {
    null
  }
  
  def getMetadata(ra:String):HashMap[String, String] = {
    
    val resource = accessEndpoint get ra
    
    if(resource != null){
    	val map = new JMap[String, String]
    
    	map ++ resource.metadata
    
    	map.underlying
    }else{
      null
    }
  }
  
}
