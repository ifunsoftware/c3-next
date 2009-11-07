package org.aphreet.c3.platform.storage.bdb

import java.util.Date

import org.aphreet.c3.platform.resource._
import org.aphreet.c3.platform.storage.common.AbstractBDBStorage

class MutableBDBStorage(override val id:String, override val path:String) extends AbstractBDBStorage(id, path){

  override protected def preSave(resource:Resource){
    
    resource.isMutable = true
    
    for(version <- resource.versions if (version.persisted == false)){
      version.systemMetadata.put(
        Resource.MD_EMBEDDED_CONTENT, version.data.stringValue)
    }
    
  }
  
  def fillResourceWithData(resource:Resource) = {
    for(version <- resource.versions){
      version.data = version.systemMetadata.get(Resource.MD_EMBEDDED_CONTENT) match {
        case Some(value:String) => DataWrapper.wrap(value)
        case None => DataWrapper.empty
      }
    }
  }
  
  def name = MutableBDBStorage.NAME
  
  def storageType:StorageType.Value = StorageType.MUTABLE
  
}

object MutableBDBStorage{
  val NAME = classOf[MutableBDBStorage].getSimpleName
}