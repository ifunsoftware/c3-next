package org.aphreet.c3.platform.storage.dispatcher.selector.size

import org.aphreet.c3.platform.resource.Resource

import scala.collection.immutable.{SortedMap, TreeMap}

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.PostConstruct

@Component
class SizeStorageSelector extends StorageSelector{

  private var sizeRanges:SortedMap[Long, String] = null
  
  private var configAccessor:SizeSelectorConfigAccessor = null
  
  @Autowired
  def setSizeSelectorConfigAccessor(accessor:SizeSelectorConfigAccessor) = {configAccessor = accessor}
  
  @PostConstruct
  def init = updateCachedConfig
  
  def storageTypeForResource(resource:Resource):String = {
    
    val size = resource.versions(0).data.length
    storageTypeForSize(size)
  }
  
  def storageTypeForSize(size:Long):String = {
    for(sizeRange <- sizeRanges){
      if(size > sizeRange._1)
        return sizeRange._2
    }
    null
  }
  
  def addEntry(entry:(Long, String)) = {
    configAccessor.update(_ + entry)
    updateCachedConfig
  }
  
  def removeEntry(size:Long) = {
    configAccessor.update(_.filter(_._1 != size))
    updateCachedConfig
  }
  
  def entries:List[(Long, String)] = sizeRanges.toList
  
  private def updateCachedConfig = {
    sizeRanges = new TreeMap[Long, String]()(new ReverceOrdered(_)) ++ configAccessor.load
  }
  
}

class ReverceOrdered(val value:Long) extends Ordered[Long] {
  
  override def compare(x:Long):Int = {
    if(x > value) return 1
    if(x < value) return -1
    0
  }
}
