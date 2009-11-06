package org.aphreet.c3.platform.storage.dispatcher.impl

import org.aphreet.c3.platform.resource.{Resource, DataWrapper}

import org.aphreet.c3.platform.storage.StorageType._

import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeSet

class DefaultStorageDispatcher(sts:List[Storage]) extends StorageDispatcher {

  val storages = new HashMap[String, List[Storage]]
  
  val typeMapping = new HashMap[String, String]
  
  val sizeRanges = List((500000, "FileStorage"))
  
  val default = "FixedBDBStorage"
  
  { 
    
    typeMapping.put("application/c3-wiki", "MutableBDBStorage")
    typeMapping.put("application/c3-message", "FixedBDBStorage")
    
    for(s <- sts){
      storages.get(s.name) match {
        case Some(xs) => storages.put(s.name, xs ::: List(s))
        case None => storages.put(s.name, List(s))
      } 
    }
  }
  
  def selectStorageForResource(resource:Resource):Storage = {
    
    val contentType = resource.systemMetadata.get(Resource.MD_CONTENT_TYPE) match {
      case Some(x) => x
      case None => ""
    }
    
    val storageName = typeMapping.get(contentType) match {
      case Some(name) => name
      case None => selectStorageForSize(resource.versions(0).data.length)
    }
    
    selectStorageForName(storageName)
  }
  
  private def selectStorageForSize(size:Long):String = {
    for(sizeRange <- sizeRanges){
      if(size > sizeRange._1)
        return sizeRange._2
    }
    default
  }
  
  private def selectStorageForName(name:String):Storage = {
    var storage = storages.get(name) match {
      case Some(sx) => random(sx)
      case None => null
    }
    
    if(storage == null){
      storage = storages.get(default) match {
        case Some(sx) => random(sx)
        case None => null
      }
    }
    
    storage
    
  }
  
  private def random(list:List[Storage]):Storage = {
    val onlineList = list.filter(s => s.mode == RW)
    
    if(onlineList.isEmpty){
      null
    }else{
      val num = (new scala.util.Random).nextInt % (onlineList.size)
      onlineList.drop(num).first
    }
  }
}
