package org.aphreet.c3.platform.storage.common

import org.aphreet.c3.platform.common.{Path, Constants}
import org.aphreet.c3.platform.resource.{Resource}

import java.io.{File}

import com.sleepycat.je.{EnvironmentConfig, Environment, DatabaseConfig, Database, DatabaseEntry, LockMode, OperationStatus, Transaction}
import org.aphreet.c3.platform.storage.{U, StorageIterator}
import org.aphreet.c3.platform.exception.StorageException

abstract class AbstractBDBStorage(val storageId:String, override val path:Path) extends AbstractStorage(storageId, path){

  protected var env : Environment = null

  protected var objectCount:Long = -1

  var database : Database = null

  protected val storageName:String = name + "-" + id

  protected val storagePath:String = path.toString + "/" + storageName

  {
    val envConfig = new EnvironmentConfig
    envConfig setAllowCreate true
    envConfig setSharedCache true
    envConfig setTransactional true
    envConfig setCachePercent 20

    val storagePathFile = new File(storagePath, "metadata")
    if(!storagePathFile.exists){
      storagePathFile.mkdirs
    }

    env = new Environment(storagePathFile, envConfig)

    val dbConfig = new DatabaseConfig
    dbConfig setAllowCreate true
    dbConfig setTransactional true
    database = env.openDatabase(null, storageName, dbConfig)
  }

  def count:Long = objectCount;

  override protected def updateObjectCount = {
    val cnt = database.count
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

  def add(resource:Resource):String = {

    val ra = generateName

    resource.address = ra

    preSave(resource)

    val tx = env.beginTransaction(null, null)

    try{
      storeData(resource, tx)

      val key = new DatabaseEntry(ra.getBytes)
      val value = new DatabaseEntry(resource.toByteArray)

      if(database.putNoOverwrite(tx, key, value) != OperationStatus.SUCCESS){
        throw new StorageException("Failed to store resource in database")
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

  def get(ra:String):Option[Resource] = {

    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    if(database.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS){
      val resource = Resource.fromByteArray(value.getData)
      resource.address = ra
      loadData(resource)
      Some(resource)
    }else None

  }

  def update(resource:Resource):String = {
    val ra = resource.address

    preSave(resource)

    val tx = env.beginTransaction(null, null)

    try{
      storeData(resource, tx)

      val key = new DatabaseEntry(ra.getBytes)
      val value = new DatabaseEntry(resource.toByteArray)

      if(database.put(tx, key, value) != OperationStatus.SUCCESS){
        throw new StorageException("Failed to store resource in database")
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

  def delete(ra:String) = {
    val key = new DatabaseEntry(ra.getBytes)

    val tx = env.beginTransaction(null, null)
    try{
      deleteData(ra, tx)
      database.delete(tx, key)

      tx.commit
    }catch{
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def put(resource:Resource) = {

    val tx = env.beginTransaction(null, null)

    try{
      putData(resource, tx)

      val key = new DatabaseEntry(resource.address.getBytes)
      val value = new DatabaseEntry(resource.toByteArray)

      if(database.put(tx, key, value) != OperationStatus.SUCCESS){
        throw new StorageException("Failed to store resource in database")
      }

      tx.commit
    }catch{
      case e=> {
        tx.abort
        throw e
      }
    }
  }

  def isAddressExists(address:String):Boolean = {

    val key = new DatabaseEntry(address.getBytes)
    val value = new DatabaseEntry()

    database.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS
  }

  def iterator:StorageIterator = new BDBStorageIterator(this)


  override def close = {
    super.close
    if(this.mode.allowRead)
      mode = U(Constants.STORAGE_MODE_NONE)


    if(database != null){
      database.close
      database = null
    }

    if(env != null){
      env.cleanLog;
      env.close
      env = null
    }
  }

  protected def storeData(resource:Resource, tx:Transaction){
    storeData(resource)
  }

  protected def storeData(resource:Resource){}


  protected def deleteData(ra:String, tx:Transaction){
    deleteData(ra)
  }

  protected def deleteData(ra:String){}


  protected def putData(resource:Resource){}

  protected def putData(resource:Resource, tx:Transaction){
    putData(resource)
  }

  def loadData(resource:Resource)

  protected def preSave(resource:Resource){}

  protected def postSave(resource:Resource){
    for(version <- resource.versions if !version.persisted){
      version.persisted = true
    }
  }
}

