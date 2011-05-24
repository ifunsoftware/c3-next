package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.common.{Path, Constants}
import org.aphreet.c3.platform.resource.Resource

import java.io.File

import org.aphreet.c3.platform.exception.{ResourceNotFoundException, StorageException}
import org.aphreet.c3.platform.storage.{StorageParams, StorageIterator}
import com.sleepycat.je._
import collection.mutable.{HashSet, HashMap}
import org.aphreet.c3.platform.storage.common.AbstractStorage

abstract class AbstractBDBStorage(override val parameters:StorageParams,
                                  override val systemId:String,
                                  val config:BDBConfig) extends AbstractStorage(parameters, systemId){


  protected var objectCount:Long = -1

  protected val storageName:String = name + "-" + id

  protected val storagePath:String = path.toString + "/" + storageName

  val iterators = new HashSet[BDBStorageIterator]


  def count:Long = objectCount;

  override protected def updateObjectCount = {
    log trace "Updating object count"
    val cnt = getDatabase(true).count
    this.synchronized{
      objectCount = cnt
    }

  }

  def size:Long = calculateSize(new File(storagePath))

  def fullPath:Path = new Path(storagePath)


  private def calculateSize(dir:File):Long = {
    var size:Long = 0

    for(child <- dir.listFiles){
      if(!child.isDirectory){
        size = size + child.length
      }else{
        size = size + calculateSize(child)
      }
    }

    size
  }

  protected def failuresArePossible(block: => Any):Unit

  def isAddressExists(address:String):Boolean = {

    val key = new DatabaseEntry(address.getBytes)
    val value = new DatabaseEntry()

    getDatabase(false).get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS
  }

  def get(ra:String):Option[Resource] = {

    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    if (getDatabase(false).get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      val resource = Resource.fromByteArray(value.getData)
      resource.address = ra
      loadData(resource)
      Some(resource)
    } else None

  }

  def add(resource:Resource):String = {

    val tx = getEnvironment.beginTransaction(null, null)

    val ra = generateName(TransactionBasedSeedSource(tx))

    resource.address = ra

    preSave(resource)

    try{
      storeData(resource, tx)

      val key = new DatabaseEntry(ra.getBytes)
      val value = new DatabaseEntry(resource.toByteArray)

      failuresArePossible{

        val status = getDatabase(true).putNoOverwrite(tx, key, value)

        if(status != OperationStatus.SUCCESS){
          throw new StorageException("Failed to store resource in database, operation status is: " + status.toString + "; address: " + ra)
        }
      }

      tx.commit

      postSave(resource)

      ra
    }catch{
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def put(resource:Resource) = {

    val tx = getEnvironment.beginTransaction(null, null)

    try{
      putData(resource, tx)

      val key = new DatabaseEntry(resource.address.getBytes)
      val value = new DatabaseEntry(resource.toByteArray)

      failuresArePossible{
        val status = getDatabase(true).put(tx, key, value)

        if(status != OperationStatus.SUCCESS){
          throw new StorageException("Failed to store resource in database, operation status: " + status.toString)
        }
      }

      tx.commit
    }catch{
      case e=> {
        tx.abort
        throw e
      }
    }
  }

  def delete(ra:String) = {
    val key = new DatabaseEntry(ra.getBytes)

    val tx = getEnvironment.beginTransaction(null, null)
    try{
      deleteData(ra, tx)

      failuresArePossible{

        val status = getDatabase(true).delete(tx, key)

        if(status != OperationStatus.SUCCESS)
          throw new StorageException("Failed to delete data from DB, op status: " + status.toString)

      }
      tx.commit
    }catch{
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def lock(ra:String){
    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    val tx = getEnvironment.beginTransaction(null, null)

    try{
      val status = getDatabase(true).get(tx, key, value, LockMode.RMW)
      if(status == OperationStatus.SUCCESS){
        val res = Resource.fromByteArray(value.getData)
        res.systemMetadata.get(Resource.SMD_LOCK) match{
          case Some(x) => throw new StorageException("Failed to obtain lock")
          case None =>
        }

        res.systemMetadata.put(Resource.SMD_LOCK, System.currentTimeMillis.toString)

        value.setData(res.toByteArray)

        failuresArePossible{
          getDatabase(true).put(tx, key, value)
        }
        
        tx.commit
      } else {
        throw new ResourceNotFoundException(
          "Failed to get resource with address " + ra + " Operation status " + status.toString)
      }
    } catch {
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def unlock(ra:String){
    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    val tx = getEnvironment.beginTransaction(null, null)

    try{
      val status = getDatabase(true).get(tx, key, value, LockMode.RMW)

      if(status == OperationStatus.SUCCESS){
        val res = Resource.fromByteArray(value.getData)


        res.systemMetadata.remove(Resource.SMD_LOCK)

        value.setData(res.toByteArray)

        failuresArePossible{
          getDatabase(true).put(tx, key, value)
        }

        tx.commit
      }else{
        throw new ResourceNotFoundException(
          "Failed to get resource with address " + ra + " Operation status " + status.toString)
      }
    }catch{
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def appendSystemMetadata(ra:String, metadata:Map[String, String]){

    val tx = getEnvironment.beginTransaction(null, null)

    try{

      //Obtaining actual version of resource and locking it for write
      val savedResource:Resource = {
        val key = new DatabaseEntry(ra.getBytes)
        val value = new DatabaseEntry()

        val status = getDatabase(true).get(tx, key, value, LockMode.RMW)
        if(status == OperationStatus.SUCCESS){
          val res = Resource.fromByteArray(value.getData)
          res.address = ra
          res
        }else throw new ResourceNotFoundException(
          "Failed to get resource with address " + ra + " Operation status " + status.toString)
      }
      //Appending system metadata
      savedResource.systemMetadata ++= metadata

      val key = new DatabaseEntry(ra.getBytes)
      val value = new DatabaseEntry(savedResource.toByteArray)

      failuresArePossible{
        if(getDatabase(true).put(tx, key, value) != OperationStatus.SUCCESS){
          throw new StorageException("Failed to store resource in database")
        }
      }

      tx.commit
    }catch{
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def update(resource:Resource):String = {
    val ra = resource.address

    preSave(resource)

    val tx = getEnvironment.beginTransaction(null, null)


    try {

      //Obtaining actual version of resource and locking it for write
      val savedResource:Resource = {
        val key = new DatabaseEntry(ra.getBytes)
        val value = new DatabaseEntry()

        val status = getDatabase(true).get(tx, key, value, LockMode.RMW)
        if(status == OperationStatus.SUCCESS){
          val res = Resource.fromByteArray(value.getData)
          res.address = ra
          res
        }else throw new ResourceNotFoundException(
          "Failed to get resource with address " + ra + " Operation status " + status.toString)
      }

      //Replacing metadata
      savedResource.metadata.clear
      savedResource.metadata ++= resource.metadata

      //Appending system metadata
      savedResource.systemMetadata ++= resource.systemMetadata


      for(version <- resource.versions if !version.persisted)
        savedResource.addVersion(version)


      storeData(savedResource, tx)

      val key = new DatabaseEntry(ra.getBytes)
      val value = new DatabaseEntry(savedResource.toByteArray)

      failuresArePossible{
        if(getDatabase(true).put(tx, key, value) != OperationStatus.SUCCESS){
          throw new StorageException("Failed to store resource in database")
        }
      }

      tx.commit

      postSave(savedResource)

      ra
    }catch{
      case e => {
        tx.abort
        throw e
      }
    }
  }



  def iterator(fields:Map[String,String],
               systemFields:Map[String,String],
               filter:Function1[Resource, Boolean]):StorageIterator = {

    if(log.isDebugEnabled){
      log debug "Creating iterator; fields: " + fields + " sysFields: " + systemFields
    }

    val iterator = new BDBStorageIterator(this, fields, systemFields, filter)

    iterators.synchronized(
      iterators += iterator
    )

    log debug "Iterator created"

    iterator

  }

  def removeIterator(iterator:BDBStorageIterator){
    iterators.synchronized(
      iterators -= iterator
    )
  }

  def loadData(resource:Resource)


  protected def storeData(resource:Resource, tx:Transaction):Unit = storeData(resource)

  protected def storeData(resource:Resource):Unit = {}

  protected def deleteData(ra:String, tx:Transaction):Unit = deleteData(ra)

  protected def deleteData(ra:String):Unit = {}

  protected def putData(resource:Resource):Unit = {}

  protected def putData(resource:Resource, tx:Transaction):Unit = putData(resource)


  protected def preSave(resource:Resource){}

  protected def postSave(resource:Resource){
    for(version <- resource.versions if !version.persisted){
      version.persisted = true
    }
  }

  def getDatabase(writeFlag : Boolean) : Database

  def getSecondaryDatabases(writeFlag : Boolean) : HashMap[String, SecondaryDatabase]

  protected def getEnvironment() : Environment

  //protected def open(bdbConfig : BDBConfig){}
}

