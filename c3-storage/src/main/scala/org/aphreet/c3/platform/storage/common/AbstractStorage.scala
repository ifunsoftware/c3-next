package org.aphreet.c3.platform.storage.common

import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.resource.IdGenerator
import org.aphreet.c3.platform.storage.{StorageIndex, Storage, StorageParams}
import org.aphreet.c3.platform.common.{ThreadWatcher, Path}
import collection.mutable.HashMap

abstract class AbstractStorage(val parameters:StorageParams, val systemId:String) extends Storage{

  protected var counter:Thread = null

  def id:String = parameters.id

  def path:Path = parameters.path

  var indexes = parameters.indexes

  def params:StorageParams = {
    new StorageParams(id, ids, parameters.path, parameters.storageType, parameters.mode, indexes, new HashMap[String, String])
  }

  def startObjectCounter = {
    counter = new Thread(new ObjectCounter(this))
    counter.setDaemon(true)
    counter.start
    log info "Started object counter for storage " + this.id
  }

  def generateName(seedSource:SeedSource):String = {

    val seed = seedSource.getSeed

    var address = IdGenerator.generateAddress(seed, systemId, id)

    while(isAddressExists(address)){
      address = IdGenerator.generateAddress(seed, systemId, id)
    }

    address
  }

  def isAddressExists(address:String):Boolean

  protected def updateObjectCount;

  override def close{
    counter.interrupt
  }

  class ObjectCounter(val storage:AbstractStorage) extends Runnable {

    override def run{
      ThreadWatcher + this
      try{

        try{
          Thread.sleep(60 * 1000)
        }catch{
          case e => {
            log info "Object counter for storage " + storage.id + " interrupted on start"
            return
          }
        }
        while(!Thread.currentThread.isInterrupted){
          storage.updateObjectCount
          try{
            Thread.sleep(60 * 1000)
          }catch{
            case e:InterruptedException => {
              log.info("Object counter for storage " + storage.id + " has been interrupted")
              return
            }
          }
        }
      }finally{
        ThreadWatcher - this
      }

    }

  }

}


