package org.aphreet.c3.platform.storage.common

import org.aphreet.c3.platform.common.{Path, Constants}
import org.aphreet.c3.platform.resource.Resource

import java.io.File

import org.aphreet.c3.platform.exception.{ResourceNotFoundException, StorageException}
import org.aphreet.c3.platform.storage.{StorageParams, StorageIterator}
import com.sleepycat.je._
import collection.mutable.{HashSet, HashMap}

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

