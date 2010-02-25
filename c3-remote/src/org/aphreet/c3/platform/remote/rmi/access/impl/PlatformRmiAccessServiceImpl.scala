package org.aphreet.c3.platform.remote.rmi.access

import java.util.HashMap

import scala.collection.jcl.{HashMap => JMap}

import org.aphreet.c3.platform.access.PlatformAccessEndpoint
import org.aphreet.c3.platform.resource.{DataWrapper, Resource, ResourceVersion}
import org.aphreet.c3.platform.remote.api.rmi.access.PlatformRmiAccessService

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.remote.api.access.PlatformAccessAdapter

@Component("platformRmiAccessService")
class PlatformRmiAccessServiceImpl extends PlatformRmiAccessService{

  var accessEndpoint:PlatformAccessEndpoint = null

  var accessAdapter:PlatformAccessAdapter = null
  
  @Autowired
  def setAccessEndpoint(endpoint:PlatformAccessEndpoint) = {accessEndpoint = endpoint}

  @Autowired
  def setAccessAdapter(adapter:PlatformAccessAdapter) = {accessAdapter = adapter}
 
  def add(metadata:HashMap[String, String], data:Array[Byte]):String = {
    
    val resource = new Resource
    resource.metadata ++ new JMap(metadata)
    
    val version = new ResourceVersion
    version.data = DataWrapper.wrap(data)
    resource addVersion version
    
    accessEndpoint.add(resource)
  }
  
  def getResourceAsString(ra:String):String = accessAdapter.getResourceAsString(ra)
  
  def getMetadata(ra:String):HashMap[String, String] = accessAdapter.getMetadata(ra)
  
}
