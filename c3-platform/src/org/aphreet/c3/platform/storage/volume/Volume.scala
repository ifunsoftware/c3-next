package org.aphreet.c3.platform.storage.volume;

import scala.collection.mutable.{Set, HashSet}

import org.aphreet.c3.platform.common.Constants
import org.aphreet.c3.platform.exception.PropertyChangeException
import org.aphreet.c3.platform.storage._

import org.apache.commons.logging.LogFactory


class Volume(val mountPoint:String, var size:Long, var available:Long){
  
  private var lowLimit  = 100000000l;
  
  private var highLimit = 500000000l;
  
  val storages:Set[Storage] = new HashSet
  
  def safeAvailable:Long = available - lowLimit
  
  def updateState(s:Long, a:Long){
    size = s;
    available = a;
    
    val moveToRO = available < lowLimit
    var moveToRW = available > highLimit
    
    for(storage <- storages){
      if(moveToRO)
        if(storage.mode != U && storage.mode != RO){
          Volume.logger.info("moving storage " + storage.id + "to RO")
          storage.mode = RO(Constants.STORAGE_MODE_CAPACITY)
        }
      
      if(moveToRW)
        if(storage.mode == RO){
          Volume.logger.info("moving storage " + storage.id + "to RW")
          storage.mode = RW("")
        }
    }
    
  }
  
  def setLowLimit(value:Long) = {
    lowLimit = value
  }
  
  def setHighLimit(value:Long) = {
    highLimit = value
  }
  
  override def toString:String = "Volume[" +mountPoint + " " + size + " " + available + "]"

}

object Volume{
	val logger = LogFactory getLog Volume.getClass
}

