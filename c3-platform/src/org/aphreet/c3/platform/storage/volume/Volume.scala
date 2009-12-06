package org.aphreet.c3.platform.storage.volume;

import scala.collection.mutable.{Set, HashSet}

import org.aphreet.c3.platform.exception.PropertyChangeException

import org.apache.commons.logging.LogFactory

class Volume(val mountPoint:String, var size:Long, var available:Long){
  
  private var lowWatermark  = 100000000l;
  
  private var highWatermark = 500000000l;
  
  val storages:Set[Storage] = new HashSet
  
  def updateState(s:Long, a:Long){
    size = s;
    available = a;
    
    val moveToRO = available < lowWatermark
    var moveToRW = available > highWatermark
    
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
  
  def setLowWatermark(value:Long) = {
    if(value > highWatermark){
      throw new PropertyChangeException("Low Watermark must be lower than High Watermark")
    }
    lowWatermark = value
  }
  
  def setHighWatermark(value:Long) = {
    if(value < lowWatermark){
      throw new PropertyChangeException("High Watermark must be higher than Low Watermark" )
    }
    highWatermark = value
  }
  
  override def toString:String = "Volume[" +mountPoint + " " + size + " " + available + "]"

}

object Volume{
	val logger = LogFactory getLog Volume.getClass
}

