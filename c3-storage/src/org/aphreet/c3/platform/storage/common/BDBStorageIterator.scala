package org.aphreet.c3.platform.storage.common

import com.sleepycat.je._

import org.aphreet.c3.platform.storage.StorageIterator
import org.aphreet.c3.platform.resource.{AddressGenerator, Resource}
import org.aphreet.c3.platform.exception.StorageException

class BDBStorageIterator(storage:AbstractBDBStorage) extends StorageIterator{

  val RESOURCE_ADDRESS_LENGTH = 41

  var cursor:Cursor = storage.database.openCursor(null, null)

  private var bdbEntriesProcessed = 0

  private var resource:Resource = null

  {
    resource = findNextResource
  }


  def hasNext:Boolean = resource != null

  def next:Resource = {

    val previousResource = resource

    resource = findNextResource

    previousResource
  }

  protected def findNextResource:Resource = {

    var resource:Resource = null

    var resultFound = false


    while(!resultFound){

      if(!storage.mode.allowRead){
        this.close
        throw new StorageException("Storage " + storage.id + " is not readable")
      }

      val databaseKey = new DatabaseEntry
      val databaseValue = new DatabaseEntry

      if(cursor.getNext(databaseKey, databaseValue, LockMode.DEFAULT) == OperationStatus.SUCCESS){
        val key = new String(databaseKey.getData)

        if(AddressGenerator.isValidAddress(key)){
          resource = Resource.fromByteArray(databaseValue.getData)
          loadData(resource)
          resultFound = true
        }
        bdbEntriesProcessed = bdbEntriesProcessed + 1;
      }else{
        resource = null
        resultFound = true
      }
    }

    resource
  }


  protected def loadData(resource:Resource) = storage.loadData(resource)

  override def objectsProcessed:Int = bdbEntriesProcessed

  def close = {
    try{
      cursor.close
      cursor = null
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
