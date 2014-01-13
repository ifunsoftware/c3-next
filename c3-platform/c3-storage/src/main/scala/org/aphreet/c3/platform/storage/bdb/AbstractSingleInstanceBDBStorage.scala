/**
 * Copyright (c) 2010, Mikhail Malygin, Anton Krasikov
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.aphreet.c3.platform.storage.bdb

import java.io.File
import com.sleepycat.je._
import org.aphreet.c3.platform.common.Constants
import org.aphreet.c3.platform.storage.{ConflictResolverProvider, U, StorageIndex, StorageParams}
import java.util.concurrent.TimeUnit
import collection.mutable

abstract class AbstractSingleInstanceBDBStorage (override val parameters: StorageParams,
                     override val systemId:String,
                     override val config: BDBConfig,
                     override val conflictResolverProvider: ConflictResolverProvider)
  extends AbstractBDBStorage(parameters, systemId, config, conflictResolverProvider) {

  protected var env : Environment = null

  var database : Database = null

  val secondaryDatabases = new mutable.HashMap[String, SecondaryDatabase]


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
    envConfig setClassLoader(getClass.getClassLoader)

    if(params.params.contains(AbstractBDBStorage.USE_SHORT_LOCK_TIMEOUT)){
      envConfig.setLockTimeout(5, TimeUnit.SECONDS)
    }else{
      envConfig.setLockTimeout(5, TimeUnit.MINUTES)
    }

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

      val keyCreator = new C3SecondaryKeyCreator(index)
      secConfig.setKeyCreator(keyCreator)

      keyCreator.comparator match {
        case Some(comparator) => secConfig.setBtreeComparator(comparator)
        case None =>
      }

      val secDatabase = env.openSecondaryDatabase(null, index.name, database, secConfig)

      secondaryDatabases.put(index.name, secDatabase)
    }

    log info "Storage " + id + " opened"

    startObjectCounter()
  }

  def createIndex(index:StorageIndex){
    val secConfig = new SecondaryConfig
    secConfig setAllowCreate true
    secConfig setTransactional true
    secConfig setSortedDuplicates true

    val keyCreator = new C3SecondaryKeyCreator(index)
    secConfig.setKeyCreator(keyCreator)

    keyCreator.comparator match {
      case Some(comparator) => secConfig.setBtreeComparator(comparator)
      case None =>
    }

    //This is mandatory as we create an index when db may already contain data
    secConfig setAllowPopulate true

    val secDatabase = env.openSecondaryDatabase(null, index.name, database, secConfig)

    secondaryDatabases.put(index.name, secDatabase)

    indexes = index :: indexes
  }

  def removeIndex(index:StorageIndex){
    val idxName = index.name

    secondaryDatabases.get(idxName) match{
      case None => {}
      case Some(secDb) => {
        secDb.close()
        env.removeDatabase(null, idxName)
      }
    }

    indexes = indexes.filter(_.name != index.name)
  }

  override def close() {
    log info "Closing storage " + id
    super.close()
    if(this.mode.allowRead)
      mode = U(Constants.STORAGE_MODE_NONE)


    try{

      log info "Closing iterators..."

      val iteratorList = iterators.toList

      for(iterator <- iteratorList){
        iterator.close()
      }

    }catch{
      case e: Throwable => log.error("Failed to close iterator: ", e)
    }


    for((name, secDb) <- secondaryDatabases){
      secDb.close()
    }

    secondaryDatabases.clear()

    if(database != null){
      database.close()
      database = null
    }

    if(env != null){
      env.cleanLog
      env.close()
      env = null
    }

    log info "Storage " + id + " closed"
  }


  override def getDatabase(writeFlag : Boolean) : Database = {
    database
  }

  override def secondaryDatabases(writeFlag : Boolean) : mutable.HashMap[String, SecondaryDatabase] = {
    secondaryDatabases
  }

  override def environment: Environment = {
    env
  }

}