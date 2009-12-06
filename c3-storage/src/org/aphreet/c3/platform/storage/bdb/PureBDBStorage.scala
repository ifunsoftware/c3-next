package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.resource._
import org.aphreet.c3.platform.storage.common.AbstractBDBStorage

import com.sleepycat.je._

class PureBDBStorage(override val id:String, override val path:Path) extends AbstractBDBStorage(id, path) {

  override protected def storeData(resource:Resource, tx:Transaction){
    
    def storeVersionData(key:String, version:ResourceVersion){
      
      val dbKey = new DatabaseEntry(key.getBytes)
	  val dbValue = new DatabaseEntry(version.data.getBytes)
	   
	  database.put(tx, dbKey, dbValue)
    }
    
    if(resource.isVersioned){
	  for(version <- resource.versions){
	    if(version.persisted == false){
	      val versionKey = resource.address + "-data-" + String.valueOf(System.currentTimeMillis)
	      version.systemMetadata.put(Resource.MD_DATA_ADDRESS, versionKey)
          storeVersionData(versionKey, version)
	    }
	  }
    }else{
      val versionKey = resource.address + "-data"
      resource.versions(0).systemMetadata.put(Resource.MD_DATA_ADDRESS, versionKey)
      storeVersionData(versionKey, resource.versions(0))
    }
  }
  
  def loadData(resource:Resource) = {
    for(version <- resource.versions){
      
      val versionKey = version.systemMetadata.get(Resource.MD_DATA_ADDRESS) match {
        case Some(value:String) => value
        case None => throw new StorageException("Can't find data reference for version in resource: " + resource.address)
      }
      
      val key = new DatabaseEntry(versionKey.getBytes)
      val value = new DatabaseEntry()
    
      if(database.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS)
        version.data = DataWrapper.wrap(value.getData)
      else
        throw new StorageException("Failed to get data from MutableBDBStorage, operation status is not SUCCESS for resource: " + resource.address + " and version: " + versionKey)
    }
  }
  
  
  override def deleteData(ra:String, tx:Transaction){
    
    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()
    
    if(database.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS){
      val resource = Resource.fromByteArray(value.getData)
      
      for(version <- resource.versions){
        val dataKey = version.systemMetadata.get(Resource.MD_DATA_ADDRESS) match {
          case Some(address) => new DatabaseEntry(address.getBytes)
          case None => throw new StorageException("No data address in version for resource: " + ra)
        }
        
        database.delete(tx, dataKey); 
      }
      
      
    }else throw new StorageException("Failed to get resource, operation status is not SUCCESS, address: " + ra)
    
  }
  
  def name = PureBDBStorage.NAME
  
}

object PureBDBStorage{
  val NAME = classOf[PureBDBStorage].getSimpleName
}
