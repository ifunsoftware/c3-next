package org.aphreet.c3.platform.storage.bdb

import java.util.Date

import org.aphreet.c3.platform.resource._
import org.aphreet.c3.platform.storage.common.AbstractBDBStorage

class MutableBDBStorage(override val id:String, override val path:String) extends AbstractBDBStorage(id, path){

  override protected def prepareMetadata(resource:Resource){
    
    val version = new ResourceVersion
    version.date = new Date
    version.systemMetadata.put(Resource.MD_EMBEDDED_CONTENT, resource.data.stringValue)
    resource.versions + version
  }
  
  def fillResourceWithData(resource:Resource) = {
    for(version <- resource.versions){
      version.data = version.systemMetadata.get(Resource.MD_EMBEDDED_CONTENT) match {
        case Some(value) => DataWrapper.wrap(value)
        case None => DataWrapper.empty
      }
    }
    resource.data = DataWrapper.empty
  }
  
  def name = MutableBDBStorage.NAME
  
  def storageType:StorageType.Value = StorageType.MUTABLE
  
}

object MutableBDBStorage{
  val NAME = classOf[MutableBDBStorage].getSimpleName
}