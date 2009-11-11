package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.resource._
import org.aphreet.c3.platform.storage.common.AbstractBDBStorage
import org.aphreet.c3.platform.storage.StorageType

import com.sleepycat.je._

class FixedBDBStorage(override val id:String, override val path:String) extends AbstractBDBStorage(id, path){
  
  override protected def storeData(resource:Resource, tx:Transaction){
    val key = new DatabaseEntry((resource.address + "-data").getBytes)
    val value = new DatabaseEntry(resource.versions(0).data.getBytes)
    
    database.put(tx, key,value)
  }
  
  def loadData(resource:Resource) = {
    
    val key = new DatabaseEntry((resource.address + "-data").getBytes)
    val value = new DatabaseEntry()
    
    if(database.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS)
      resource.versions(0).data = DataWrapper.wrap(value.getData)
    else
      throw new StorageException("Failed to get data from FixedBDBStorage, operation status is not SUCCESS for resource: " + resource.address)
    
  }
  
  override def deleteData(ra:String, tx:Transaction){
    
    val dataKey = new DatabaseEntry((ra + "-data").getBytes)    
    database.delete(tx, dataKey)
    
  }
  
  def name = FixedBDBStorage.NAME
  
  def storageType:StorageType.Value = StorageType.FIXED
  
}

object FixedBDBStorage{
  val NAME = classOf[FixedBDBStorage].getSimpleName
}
