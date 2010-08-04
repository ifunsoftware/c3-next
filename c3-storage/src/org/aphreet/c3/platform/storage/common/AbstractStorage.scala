package org.aphreet.c3.platform.storage.common

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.storage.{Storage, StorageParams}
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.resource.AddressGenerator

abstract class AbstractStorage(val id:String, val path:Path) extends Storage{

  protected var counter:Thread = null

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
  
  def params:StorageParams = new StorageParams(id, ids, path, name, mode)
  
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


