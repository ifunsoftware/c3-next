package org.aphreet.c3.platform.storage.common

import org.aphreet.c3.platform.common.{Path, Constants}
import org.aphreet.c3.platform.resource.Resource

import java.io.File

import org.aphreet.c3.platform.exception.{ResourceNotFoundException, StorageException}
import org.aphreet.c3.platform.storage.{StorageParams, StorageIndex, U, StorageIterator}
import collection.mutable.HashMap
import com.sleepycat.je._

abstract class AbstractBDBStorage(override val parameters:StorageParams,
                                  val config:BDBConfig) extends AbstractStorage(parameters){

  protected var env : Environment = null

  protected var objectCount:Long = -1

  var database : Database = null

  protected val storageName:String = name + "-" + id

  protected val storagePath:String = path.toString + "/" + storageName

  val secondaryDatabases = new HashMap[String, SecondaryDatabase]

  //Please, remove this in future!
  private var isIteratorsUsed = false

  {
   open(config)
  }

  def open(bdbConfig:BDBConfig) {
    log info "Opening storage " + id + " with config " + config

    val envConfig = new EnvironmentConfig
    envConfig setAllowCreate true
    envConfig setSharedCache true
    envConfig setTransactional true
    envConfig setCachePercent bdbConfig.cachePercent
    envConfig setTxnNoSync bdbConfig.txNoSync
    envConfig setTxnWriteNoSync bdbConfig.txWriteNoSync

    val storagePathFile = new File(storagePath, "metadata")
    if(!storagePathFile.exists){
      storagePathFile.mkdirs
    }

    env = new Environment(storagePathFile, envConfig)

    log info "Opening database..."

    val dbConfig = new DatabaseConfig
    dbConfig setAllowCreate true
    dbConfig setTransactional true
    database = env.openDatabase(null, storageName, dbConfig)

    log info "Opening secondary databases..."

    for(index <- indexes){

      log info "Index " + index.name + "..."

      val secConfig = new SecondaryConfig
      secConfig setAllowCreate true
      secConfig setTransactional true
      secConfig setSortedDuplicates true
      secConfig.setKeyCreator(new C3SecondaryKeyCreator(index))

      val secDatabase = env.openSecondaryDatabase(null, index.name, database, secConfig)

      secondaryDatabases.put(index.name, secDatabase)

    }

    log info "Storage " + id + " opened"

    startObjectCounter
  }

  def createIndex(index:StorageIndex){
     val secConfig = new SecondaryConfig
     secConfig setAllowCreate true
     secConfig setTransactional true
     secConfig setSortedDuplicates true
     secConfig.setKeyCreator(new C3SecondaryKeyCreator(index))

    val secDatabase = env.openSecondaryDatabase(null, index.name, database, secConfig)

    secondaryDatabases.put(index.name, secDatabase)

    indexes = index :: indexes
  }

  def removeIndex(index:StorageIndex){
    val idxName = index.name

    secondaryDatabases.get(idxName) match{
      case None => {}
      case Some(secDb) => {
        secDb.close
        env.removeDatabase(null, idxName)
      }
    }

    indexes = indexes.filter(_.name != index.name)
  }

  override def close = {
    log info "Closing storage " + id
    super.close
    if(this.mode.allowRead)
      mode = U(Constants.STORAGE_MODE_NONE)

    //waiting for iterators to die
    if(isIteratorsUsed) Thread.sleep(5000)

    for((name, secDb) <- secondaryDatabases){
      secDb.close
    }

    secondaryDatabases.clear

    if(database != null){
      database.close
      database = null
    }

    if(env != null){
      env.cleanLog;
      env.close
      env = null
    }

    log info "Storage " + id + " closed"
  }


  def count:Long = objectCount;

  override protected def updateObjectCount = {
    log trace "Updating object count" 
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

      val status = database.putNoOverwrite(tx, key, value)

      if(status != OperationStatus.SUCCESS){
        throw new StorageException("Failed to store resource in database, operation status is: " + status.toString)
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


      //Obtaining actual version of resource and locking it for write
      val savedResource:Resource = {
        val key = new DatabaseEntry(ra.getBytes)
        val value = new DatabaseEntry()

        val status = database.get(tx, key, value, LockMode.RMW)
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

      if(database.put(tx, key, value) != OperationStatus.SUCCESS){
        throw new StorageException("Failed to store resource in database")
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

  def delete(ra:String) = {
    val key = new DatabaseEntry(ra.getBytes)

    val tx = env.beginTransaction(null, null)
    try{
      deleteData(ra, tx)
      val status = database.delete(tx, key)

      if(status != OperationStatus.SUCCESS)
        throw new StorageException("Failed to delete data from DB, op status: " + status.toString)

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

      val status = database.put(tx, key, value)

      if(status != OperationStatus.SUCCESS){
        throw new StorageException("Failed to store resource in database, operation status: " + status.toString)
      }

      tx.commit
    }catch{
      case e=> {
        tx.abort
        throw e
      }
    }
  }

  def appendSystemMetadata(ra:String, metadata:Map[String, String]){

    val tx = env.beginTransaction(null, null)

      try{

      //Obtaining actual version of resource and locking it for write
      val savedResource:Resource = {
        val key = new DatabaseEntry(ra.getBytes)
        val value = new DatabaseEntry()

        val status = database.get(tx, key, value, LockMode.RMW)
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

      if(database.put(tx, key, value) != OperationStatus.SUCCESS){
        throw new StorageException("Failed to store resource in database")
      }

      tx.commit
    }catch{
      case e => {
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

  def iterator(fields:Map[String,String],
               systemFields:Map[String,String],
               filter:Function1[Resource, Boolean]):StorageIterator = {
    isIteratorsUsed = true
    new BDBStorageIterator(this, fields, systemFields, filter)
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
}

