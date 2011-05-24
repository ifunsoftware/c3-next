package org.aphreet.c3.platform.storage.bdb

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

  protected def failuresArePossible(block: => Any):Unit = {
    //Do nothing here
    //Nothing to do if operation falls
    block
  }

}