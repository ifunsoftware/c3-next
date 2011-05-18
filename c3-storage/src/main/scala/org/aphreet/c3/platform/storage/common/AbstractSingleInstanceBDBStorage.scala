package org.aphreet.c3.platform.storage.common

import java.io.File
import com.sleepycat.je._
import org.aphreet.c3.platform.common.Constants
import collection.mutable.{HashSet, HashMap}
import org.aphreet.c3.platform.storage.{U, StorageIndex, StorageParams}
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.exception.{ResourceNotFoundException, StorageException}
import java.util.concurrent.TimeUnit

/**
 * Created by IntelliJ IDEA.
 * User: antey
 * Date: 02.05.11
 * Time: 19:56
 * To change this template use File | Settings | File Templates.
 */

abstract class AbstractSingleInstanceBDBStorage (override val parameters: StorageParams,
                     override val systemId:String,
                     override val config: BDBConfig) extends AbstractBDBStorage(parameters, systemId, config) {

  protected var env : Environment = null

  var database : Database = null

  val secondaryDatabases = new HashMap[String, SecondaryDatabase]


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
    envConfig.setLockTimeout(5, TimeUnit.MINUTES)

    if(bdbConfig.txNoSync){
      envConfig.setDurability(Durability.COMMIT_NO_SYNC)
    }else{
      envConfig.setDurability(Durability.COMMIT_SYNC)
    }

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


    try{

      log info "Closing iterators..."

      val iteratorList = iterators.toList

      for(iterator <- iteratorList){
        iterator.close
      }

    }catch{
      case e => log.error(e)
    }


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


  override def getDatabase(writeFlag : Boolean) : Database = {
    database
  }

  override def getSecondaryDatabases(writeFlag : Boolean) : HashMap[String, SecondaryDatabase] = {
    secondaryDatabases
  }

  override def getEnvironment() : Environment = {
    env
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

      val status = getDatabase(true).putNoOverwrite(tx, key, value)

      if(status != OperationStatus.SUCCESS){
        throw new StorageException("Failed to store resource in database, operation status is: " + status.toString + "; address: " + ra)
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

    if(getDatabase(true).get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS){
      val resource = Resource.fromByteArray(value.getData)
      resource.address = ra
      loadData(resource)
      Some(resource)
    }else None

  }

  def update(resource:Resource):String = {
    val ra = resource.address

    preSave(resource)

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

      if(getDatabase(true).put(tx, key, value) != OperationStatus.SUCCESS){
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

    val tx = getEnvironment.beginTransaction(null, null)
    try{
      deleteData(ra, tx)
      val status = getDatabase(true).delete(tx, key)

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

    val tx = getEnvironment.beginTransaction(null, null)

    try{
      putData(resource, tx)

      val key = new DatabaseEntry(resource.address.getBytes)
      val value = new DatabaseEntry(resource.toByteArray)

      val status = getDatabase(true).put(tx, key, value)

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

      if(getDatabase(true).put(tx, key, value) != OperationStatus.SUCCESS){
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

        getDatabase(true).put(tx, key, value)

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

        getDatabase(true).put(tx, key, value)

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

  def isAddressExists(address:String):Boolean = {

    val key = new DatabaseEntry(address.getBytes)
    val value = new DatabaseEntry()

    getDatabase(true).get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS
  }

}