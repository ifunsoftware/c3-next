package org.aphreet.c3.platform.storage.common

import org.aphreet.c3.platform.common.Path
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.resource.AddressGenerator
import org.aphreet.c3.platform.storage.{StorageIndex, Storage, StorageParams}

abstract class AbstractStorage(val parameters:StorageParams) extends Storage{

  protected var counter:Thread = null

  def id:String = parameters.id

  def path:Path = parameters.path

  var indexes = parameters.indexes

  def params:StorageParams = {
    new StorageParams(id, parameters.secIds, parameters.path, parameters.storageType, parameters.mode, indexes)
  }

  def startObjectCounter = {
    counter = new Thread(new ObjectCounter(this))
    counter.setDaemon(true)
    counter.start
    log info "Started object counter for storage " + this.id
  }

  def generateName:String = {
    var address = AddressGenerator.addressForStorage(id)
    
    while(isAddressExists(address)){
      address = AddressGenerator.addressForStorage(id)
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

    }

  }

}


