package org.aphreet.c3.platform.storage.dispatcher.selector.size

import scala.collection.immutable.{SortedMap, TreeMap}

class SizeStorageSelector {

  private var sizeRanges:SortedMap[Long, String] = new TreeMap[Long, String]()(new ReverceOrdered(_))
  
  def selectStorageForSize(size:Long):String = {
    for(sizeRange <- sizeRanges){
      if(size > sizeRange._1)
        return sizeRange._2
    }
    null
  }
  
  
}

class ReverceOrdered(val value:Long) extends Ordered[Long] {
  
  override def compare(x:Long):Int = {
    if(x > value) return 1
    if(x < value) return -1
    0
  }
}
