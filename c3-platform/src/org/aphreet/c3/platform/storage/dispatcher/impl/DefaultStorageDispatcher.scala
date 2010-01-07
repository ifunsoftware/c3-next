package org.aphreet.c3.platform.storage.dispatcher.impl

import org.aphreet.c3.platform.resource.{Resource, DataWrapper}

import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeSet

import scala.util.matching.Regex

class DefaultStorageDispatcher(sts:List[Storage]) extends StorageDispatcher {

  val storages = new HashMap[String, List[Storage]]
  
  val typeMapping = new HashMap[String, Boolean]
  
  val sizeRanges = List((500000, "FileBDBStorage"))
  
  val default = "PureBDBStorage"
  
  { 
    
    typeMapping.put("application/c3-wiki", true)
    typeMapping.put("application/c3-message", false)
    
    for(s <- sts){
      storages.get(s.name) match {
        case Some(xs) => storages.put(s.name, xs ::: List(s))
        case None => storages.put(s.name, List(s))
      } 
    }
  }
  
  def selectStorageForResource(resource:Resource):Storage = {
    
    resource.isVersioned = resource.systemMetadata.get(Resource.MD_CONTENT_TYPE) match {
      case Some(contentType) => {
        typeMapping.get(contentType) match{
          case Some(vers) => vers
          case None => false
        }
        
      }
      case None => false
    }
    
    val storageName = selectStorageForSize(resource.versions(0).data.length)
    
    selectStorageForName(storageName)

  }
  
  private def selectStorageForSize(size:Long):String = {
    for(sizeRange <- sizeRanges){
      if(size > sizeRange._1)
        return sizeRange._2
    }
    default
  }
  
  private def selectStorageForName(name:String):Storage =
    storages.get(name) match {
      case Some(sx) => random(sx)
      case None => storages.get(default) match{
        case Some(st) => random(st)
        case None => null
      }
    }
  
  
  private def random(list:List[Storage]):Storage = {
    val onlineList = list.filter(s => s.mode.allowWrite)
    
    if(onlineList.isEmpty){
      null
    }else{
      val num = (new scala.util.Random).nextInt % (onlineList.size)
      onlineList.drop(num).first
    }
  }
}
