package org.aphreet.c3.platform.storage.common

import com.sleepycat.je._

import org.aphreet.c3.platform.resource.Resource

class BDBStorageIterator(storage:AbstractBDBStorage) extends StorageIterator{

  val cursor:Cursor = storage.database.openCursor(null, null)
  
  private var resource:Resource = null
  
  {
    val databaseKey = new DatabaseEntry
    val databaseValue = new DatabaseEntry
    
    if(cursor.getNext(databaseKey, databaseValue, LockMode.DEFAULT) == OperationStatus.SUCCESS){
      resource = Resource.fromByteArray(databaseValue.getData)
    }else{
      resource = null
    } 
  }
  
  
  def hasNext:Boolean = resource != null
  
  def next:Resource = {
    
    val previousResource = resource
    
    val databaseKey = new DatabaseEntry
    val databaseValue = new DatabaseEntry
    
    if(cursor.getNext(databaseKey, databaseValue, LockMode.DEFAULT) == OperationStatus.SUCCESS){
      resource = Resource.fromByteArray(databaseValue.getData)
    }else{
      resource = null
    }
    
    previousResource
  }
  
  protected def loadData(resource:Resource) = storage.loadData(resource)
  
  def close = {
    try{
      cursor.close
    }catch{
      case e:DatabaseException => e.printStackTrace
    }
  }
  
  override def finalize = {
    try{
      if(cursor != null) cursor.close
    }catch{
      case e=> e.printStackTrace
    }
  }
}
