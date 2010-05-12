package org.aphreet.c3.platform.access.impl

import java.io.OutputStream

import java.util.{List, Collections}

import org.aphreet.c3.platform.resource.{Resource, DataWrapper}
import org.aphreet.c3.platform.storage.ResourceAccessor
import org.aphreet.c3.platform.search.SearchManager


import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.access.PlatformAccessEndpoint

@Component("platformAccessEndpoint")
class PlatformAccessEndpointImpl extends PlatformAccessEndpoint{

  var resourceAccessor:ResourceAccessor = null
  
  var searchManager:SearchManager = null
  
  val log = org.apache.commons.logging.LogFactory.getLog(getClass)
  
  @Autowired
  def setResourceAccessor(accessor:ResourceAccessor) = {resourceAccessor = accessor}
  
  @Autowired
  def setSearchManager(manager:SearchManager) = {searchManager = manager}
  
  
  def get(ra:String):Resource = resourceAccessor.get(ra)
  
  def add(resource:Resource):String = resourceAccessor.add(resource)
  
  def update(resource:Resource):String = resourceAccessor.update(resource)
  
  def delete(ra:String) = resourceAccessor.delete(ra)
  
  def search(query:String):List[String] = {
    searchManager.search(query)
  }
  
  def query(query:String):List[String] = {
    Collections.emptyList[String]
  }
  
}
