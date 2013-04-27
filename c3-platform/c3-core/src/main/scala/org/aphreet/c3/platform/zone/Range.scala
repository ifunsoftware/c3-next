package org.aphreet.c3.platform.zone

import annotation.tailrec
import org.aphreet.c3.platform.common.Logger

class Range[E](val start:Long, val end:Long, val value:E) {

  def containsNumber(number:Long):Boolean = number >= start && number <= end

}

object RangeSet{

  val log = Logger(getClass)

}

case class RangeSet[E](ranges:List[Range[E]]){

  import RangeSet.log

  val rangeArray = ranges.toArray

  def findMappedValue(value:Long):Option[E] = {

    if(!rangeArray.isEmpty){
      searchInterval(value, rangeArray, 0, rangeArray.length)
    }else{
      None
    }
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
