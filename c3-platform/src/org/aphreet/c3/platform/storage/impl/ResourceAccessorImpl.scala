package org.aphreet.c3.platform.storage.impl

import org.aphreet.c3.platform.management.{SPlatformPropertyListener, PropertyChangeEvent}
import org.aphreet.c3.platform.search.SearchManager

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import eu.medsea.mimeutil.MimeUtil
import org.aphreet.c3.platform.storage.{StorageManager, ResourceAccessor}
import org.aphreet.c3.platform.resource.{AddressGenerator, Resource}
import org.aphreet.c3.platform.exception.{StorageIsNotWritableException, StorageException, StorageNotFoundException, ResourceNotFoundException}

@Component
class ResourceAccessorImpl extends ResourceAccessor with SPlatformPropertyListener{

  private val MIME_DETECTOR_CLASS = "c3.platform.mime.detector"

  var storageManager:StorageManager = null

  var searchManager:SearchManager = null

  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}

  @Autowired
  def setSearchManager(manager:SearchManager) = {searchManager = manager}

  def get(ra:String):Resource = {
    try{
      val storage = storageManager.storageForId(AddressGenerator.storageForAddress(ra))
  
      if(!storage.mode.allowRead){
        throw new StorageException("Storage is not readable")
      }

      storage.get(ra) match {
        case Some(r) => r
        case None => throw new ResourceNotFoundException(ra)
      }
    }catch{
      case e:StorageNotFoundException => throw new ResourceNotFoundException(e)
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
      throw new StorageNotFoundException("Failed to find storage for resource")
    }


  }

  def update(resource:Resource):String = {
    try{
      val storage = storageManager.storageForId(AddressGenerator.storageForAddress(resource.address))
      if(storage.mode.allowWrite){
        storage.update(resource)
      }else{
        throw new StorageIsNotWritableException(storage.id)
      }
    }catch{
      case e:StorageNotFoundException => throw new ResourceNotFoundException(e)
    }
  }

  def delete(ra:String) = {
    try{
      val storage = storageManager.storageForId(AddressGenerator.storageForAddress(ra))

      if(storage.mode.allowWrite)
        storage delete ra
      else
        throw new StorageIsNotWritableException(storage.id)
    }catch{
      case e:StorageNotFoundException => throw new ResourceNotFoundException(e)
    }
  }
  
  def listeningForProperties:Array[String] = Array(MIME_DETECTOR_CLASS)

  def propertyChanged(event:PropertyChangeEvent) = {

    if(event.oldValue != null){
      MimeUtil unregisterMimeDetector event.oldValue
    }

    MimeUtil registerMimeDetector event.newValue
  }

  def defaultValues:Map[String,String] =
    Map(MIME_DETECTOR_CLASS -> "eu.medsea.mimeutil.detector.ExtensionMimeDetector")

}
