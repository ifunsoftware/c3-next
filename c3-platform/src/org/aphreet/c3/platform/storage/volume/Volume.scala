package org.aphreet.c3.platform.storage.volume;

import scala.collection.mutable.{Set, HashSet}

import org.apache.commons.logging.LogFactory

class Volume(val mountPoint:String, var size:Long, var available:Long){
  
  private val LOW_WATERMARK  = 100000000l;
  
  private val HIGH_WATERMARK = 500000000l;
  
  val storages:Set[Storage] = new HashSet
  
  def updateState(s:Long, a:Long){
    size = s;
    available = a;
    
    val moveToRO = available < LOW_WATERMARK
    var moveToRW = available > HIGH_WATERMARK
    
    for(storage <- storages){
      if(moveToRO)
        if(storage.mode != U && storage.mode != RO){
          Volume.logger.info("moving storage " + storage.id + "to RO")
          storage.mode = RO
        }
      
      if(moveToRW)
        if(storage.mode == RO){
          Volume.logger.info("moving storage " + storage.id + "to RW")
          storage.mode = RW
        }
    }
    
  }
  
  
  override def toString:String = "Volume[" +mountPoint + " " + size + " " + available + "]"

}

object Volume{
	val logger = LogFactory getLog Volume.getClass
}

