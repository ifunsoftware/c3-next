package org.aphreet.c3.platform.storage.dispatcher.impl

import org.aphreet.c3.platform.resource.{Resource, DataWrapper}

import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeSet

import scala.util.matching.Regex

import org.aphreet.c3.platform.storage.dispatcher.selector.mime._
import org.aphreet.c3.platform.storage.dispatcher.selector.size._

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component
class DefaultStorageDispatcher extends StorageDispatcher {

  var storages = new HashMap[String, List[Storage]]
  
  val default = "PureBDBStorage"
  
  var mimeSelector:MimeTypeStorageSelector = null
  
  var sizeSelector:SizeStorageSelector = null
  
  @Autowired
  def setMimeTypeStorageSelector(selector:MimeTypeStorageSelector) = {mimeSelector = selector}
  
  @Autowired
  def setSizeStorageSelector(selector:SizeStorageSelector) = {sizeSelector = selector}
  
  def setStorages(sts:List[Storage]) = {
    val newStorages = new HashMap[String, List[Storage]]
    
    for(s <- sts){
      newStorages.get(s.name) match {
        case Some(xs) => newStorages.put(s.name, xs ::: List(s))
        case None => newStorages.put(s.name, List(s))
      } 
    }
    
    storages = newStorages
  }
  
  def selectStorageForResource(resource:Resource):Storage = {
    
    var storageType = mimeSelector.storageTypeForResource(resource)
    
    if(storageType == null){
    	storageType = sizeSelector.storageTypeForResource(resource)
    }
    
    if(storageType == null){
      storageType = (default, false)
    }
    
    
    resource.isVersioned = storageType._2
    
    selectStorageForName(storageType._1)
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
