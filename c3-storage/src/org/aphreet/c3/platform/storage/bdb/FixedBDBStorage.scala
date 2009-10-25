package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.resource._
import org.aphreet.c3.platform.storage.common.AbstractBDBStorage
import org.aphreet.c3.platform.storage.StorageType

class FixedBDBStorage(override val id:String, override val path:String) extends AbstractBDBStorage(id, path){

  override protected def prepareMetadata(resource:Resource){
    resource.systemMetadata.put(Resource.MD_EMBEDDED_CONTENT, resource.data.stringValue)
  }
  
  def fillResourceWithData(resource:Resource) = 
    resource.data = resource.systemMetadata.get(Resource.MD_EMBEDDED_CONTENT) match {
      case Some(value) =>  DataWrapper.wrap(value)
      case None => DataWrapper.empty
    }
  
  def name = FixedBDBStorage.NAME
  
  def storageType:StorageType.Value = StorageType.FIXED
  
}

object FixedBDBStorage{
  val NAME = classOf[FixedBDBStorage].getSimpleName
}
