package org.aphreet.c3.platform.zone

import annotation.tailrec
import org.apache.commons.logging.LogFactory

class Range[E](val start:Long, val end:Long, val value:E) {

  def containsNumber(number:Long):Boolean = number >= start && number <= end

}

case class RangeSet[E](val ranges:List[Range[E]]){

  val rangeArray = ranges.toArray

  val log = LogFactory.getLog(getClass)

  def findMappedValue(value:Long):Option[E] = {
    searchInterval(value, rangeArray, 0, rangeArray.length)
  }

  @tailrec
  private
  def searchInterval(value:Long, array:Array[Range[E]], low:Int, up:Int):Option[E] = {

    if (log.isTraceEnabled)
      log.trace("Searching in interval [" + low + ", " + up + ")")

    if(up - low == 1){

      val foundRange = rangeArray(low)

      if (foundRange.containsNumber(value)){
        Some(foundRange.value)
      }else{
        None
      }
    }else {

      val middle = low + (up - low) / 2
      val middleRange = rangeArray(middle)

      if (middleRange.start > value){
        searchInterval(value, array, low, middle)
      }else{
        searchInterval(value, array, middle, up)
      }
    }
  }

}
