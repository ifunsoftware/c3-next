package org.aphreet.c3.platform.storage.common

import org.aphreet.c3.platform.resource.{Resource, DataWrapper}

import java.io.{File, OutputStream}

import com.sleepycat.je.{EnvironmentConfig, Environment, DatabaseConfig, Database, DatabaseEntry, LockMode, OperationStatus}

abstract class AbstractBDBStorage(val storageId:String, override val path:String) extends AbstractStorage(storageId, path){

  protected var env : Environment = null
  
  var database : Database = null
  
  protected val storagePath:String = path + File.separator + storageName
  
  protected val storageName:String = name + "-" + id
  
  {
    val envConfig = new EnvironmentConfig
    envConfig setAllowCreate true
    envConfig setSharedCache true
    envConfig setTransactional true
    
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
  
  
  def add(resource:Resource):String = {
    
    val ra = generateName
    
    resource.address = ra
    
    prepareMetadata(resource)
    
    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry(resource.toByteArray)
    
    val tx = env.beginTransaction(null, null)
    try{
	    database.put(tx, key, value)
	    storeData(ra, resource.data)
     
	    tx.commit
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
      fillResourceWithData(resource)
      Some(resource)
    }else None
    
  }
  
  def update(resource:Resource):String = {
    val ra = resource.address
    
    prepareMetadata(resource)
    
    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry(resource.toByteArray)
    
    val tx = env.beginTransaction(null, null)
    
    try{
    	database.put(tx, key, value)
    
    	storeData(ra, resource.data)
     
    	tx.commit
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
    	database.delete(tx, key)
    	deleteData(ra)
    	tx.commit
    }catch{
      case e => {
        tx.abort
        throw e
      }
    }
  }
  
  
  def iterator:StorageIterator = new BDBStorageIterator(this)
  
  
  def close = {
    mode = U
    
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
  
  protected def storeData(ra:String, data:DataWrapper){
    
  }
  
  def fillResourceWithData(resource:Resource)
  
  protected def deleteData(ra:String){
    
  }
  
  protected def prepareMetadata(resource:Resource){
    
  }
    

}
