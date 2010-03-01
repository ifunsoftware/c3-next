package org.aphreet.c3.platform.storage.common

import java.util.UUID

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.storage.{Storage, StorageParams}
import org.apache.commons.logging.LogFactory

abstract class AbstractStorage(val id:String, val path:Path) extends Storage{

  val log = LogFactory.getLog(getClass)

  protected var counter:Thread = null

  {
    counter = new Thread(new ObjectCounter(this))
    counter.start
    log info "Started object counter for storage " + this.id
  }

  def generateName:String = {
    var address = UUID.randomUUID.toString + "-" + id
    
    while(isAddressExists(address)){
      address = UUID.randomUUID.toString + "-" + id
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
          log info "Object counter interrupted on start"
          return
        }
      }
      while(!Thread.currentThread.isInterrupted){
        storage.updateObjectCount        
        try{
          Thread.sleep(60 * 1000)
        }catch{
          case e:InterruptedException => {
            log.info("Object counter for storage" + storage.id + "has been interrupted")
            return
          }
        }
      }

    }

  }

}


