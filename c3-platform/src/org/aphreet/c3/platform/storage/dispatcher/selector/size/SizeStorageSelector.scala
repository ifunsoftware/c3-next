package org.aphreet.c3.platform.storage.dispatcher.selector.size

import org.aphreet.c3.platform.resource.Resource

import scala.collection.immutable.{SortedMap, TreeMap}

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component
class SizeStorageSelector extends AbstractStorageSelector[Long]{

  private var sizeRanges:SortedMap[Long, (String, Boolean)] = null
  
  @Autowired
  def setSizeSelectorConfigAccessor(accessor:SizeSelectorConfigAccessor) = {configAccessor = accessor}
  
  override def storageTypeForResource(resource:Resource):(String,Boolean) = {
    val size = resource.versions(0).data.length
    storageTypeForSize(size)
  }
  
  def storageTypeForSize(size:Long):(String,Boolean) = {
    for(sizeRange <- sizeRanges){
      if(size >= sizeRange._1)
        return sizeRange._2
    }
    null
  }
  
  override def updateConfig(config:Map[Long, (String,Boolean)]) = {
    sizeRanges = new TreeMap[Long, (String,Boolean)]()(new ReverceOrdered(_)) ++ config
  }
  
  override def configEntries:List[(Long, String, Boolean)] = {
    sizeRanges.map(entry => (entry._1, entry._2._1, entry._2._2)).toList
  }
  
}

class ReverceOrdered(val value:Long) extends Ordered[Long] {
  
  override def compare(x:Long):Int = {
    if(x > value) return 1
    if(x < value) return -1
    0
  }
}
