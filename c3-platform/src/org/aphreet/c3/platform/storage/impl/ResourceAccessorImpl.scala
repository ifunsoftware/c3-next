package org.aphreet.c3.platform.storage.impl

import org.aphreet.c3.platform.management.{PlatformPropertyListener, PropertyChangeEvent}
import org.aphreet.c3.platform.resource.{Resource, DataWrapper}
import org.aphreet.c3.platform.search.SearchManager

import java.io.OutputStream

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import eu.medsea.mimeutil.MimeUtil

@Component
class ResourceAccessorImpl extends ResourceAccessor with PlatformPropertyListener{

  private val MIME_DETECTOR_CLASS = "c3.platform.mime.detector"
  
  var storageManager:StorageManager = null
  
  var searchManager:SearchManager = null
  
  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}
  
  def setSearchManager(manager:SearchManager) = {searchManager = manager}
  
  def get(ra:String):Resource = {
    val storage = storageManager.storageForId(storageIdFromRA(ra))
    if(storage.mode == U || storage.mode == USER_U){
      throw new StorageException("Storage is not readable")
    }
    
    storage.get(ra) match {
      case Some(r) => r
      case None => throw new StorageException("Can't find resource for ra: " + ra )
    }
  }
  
  def add(resource:Resource):String = {
    
    resource.metadata.get(Resource.MD_CONTENT_TYPE) match {
      case None => resource.metadata.put(Resource.MD_CONTENT_TYPE, resource.versions(0).data.mimeType)
      case Some(x) => null
    }
    
    val storage = storageManager.dispatcher.selectStorageForResource(resource)
    
    if(storage != null){
    	val ra = storage.add(resource)
    	searchManager index resource
    	ra
    }else{
      throw new StorageException("Failed to find storage for resource")
    }
    
    
  }
  
  def update(resource:Resource):String = {
    val storage = storageManager.storageForId(storageIdFromRA(resource.address))
    if(storage.mode == RW){
    	storage.update(resource)
    }else{
      throw new StorageException("Storage is not writtable")
    }
  }
  
  def delete(ra:String) = {
     val storage = storageManager.storageForId(storageIdFromRA(ra))
     storage delete ra
  }
  
  private def storageIdFromRA(ra:String):String = {
	ra.substring(ra.length - 4, ra.length)
  }
  
  def listeningForProperties:Array[String] = Array(MIME_DETECTOR_CLASS)
  
  def propertyChanged(event:PropertyChangeEvent) = {
    
    if(event.oldValue != null){
      MimeUtil unregisterMimeDetector event.oldValue
    }
    
    MimeUtil registerMimeDetector event.newValue
    
  }
  
}
