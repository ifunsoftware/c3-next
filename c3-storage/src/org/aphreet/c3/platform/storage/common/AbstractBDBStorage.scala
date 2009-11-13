package org.aphreet.c3.platform.storage.common

import org.aphreet.c3.platform.resource.{Resource, DataWrapper}

import java.io.{File, OutputStream}

import com.sleepycat.je.{EnvironmentConfig, Environment, DatabaseConfig, Database, DatabaseEntry, LockMode, OperationStatus, Transaction}

abstract class AbstractBDBStorage(val storageId:String, override val path:String) extends AbstractStorage(storageId, path){

  protected var env : Environment = null
  
  var database : Database = null
  
  protected val storageName:String = name + "-" + id
  
  protected val storagePath:String = path + File.separator + storageName
  
  
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
    
    preSave(resource)
    
    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry(resource.toByteArray)
    
    val tx = env.beginTransaction(null, null)
    try{
	    storeData(resource, tx)
	    database.put(tx, key, value)
     
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
    
    	database.put(tx, key, value)
     
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
  
  protected def storeData(resource:Resource, tx:Transaction){
    storeData(resource)
  }
  
  protected def storeData(resource:Resource){}
  
  
  protected def deleteData(ra:String, tx:Transaction){
    deleteData(ra)
  }
  
  protected def deleteData(ra:String){}
  
  def loadData(resource:Resource)
  
  protected def preSave(resource:Resource){}
  
  protected def postSave(resource:Resource){
    for(version <- resource.versions if !version.persisted){
      version.persisted = true
    }
  }
    

}
