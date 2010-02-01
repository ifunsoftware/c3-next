package org.aphreet.c3.platform.storage.dispatcher.selector.mime

import org.aphreet.c3.platform.resource.Resource

import scala.collection.mutable.{HashMap, ArrayBuffer}

import eu.medsea.mimeutil.MimeType

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.PostConstruct

@Component
class MimeTypeStorageSelector extends AbstractStorageSelector[String]{

  private var typeMap = new HashMap[String, (String, Boolean)]
  
  @Autowired
  def setConfigAccessor(accessor:MimeTypeConfigAccessor) = {configAccessor = accessor}
  
  override def storageTypeForResource(resource:Resource):(String,Boolean) = {
    
    val mime = new MimeType(resource.versions(0).data.mimeType)

    storageTypeForMimeType(mime) 
  }
  
  def storageTypeForMimeType(mime:MimeType):(String,Boolean) = {
    val mediaType = mime.getMediaType
    val subType = mime.getSubType
    
    typeMap.get(mediaType + "/" + subType) match {
      case Some(entry) => entry
      case None => typeMap.get(mediaType + "/*") match {
        case Some(entry) => entry
        case None => typeMap.get("*/*") match {
          case Some(entry) => entry
          case None => null
        }
      }
    }
  }
  
  override def configEntries:List[(String, String, Boolean)] = 
    typeMap.map(entry => (entry._1, entry._2._1, entry._2._2)).toList
  
  
  override def updateConfig(config:Map[String, (String,Boolean)]) = {
    val map = new HashMap[String, (String,Boolean)]
    for(entry <- config)
      map + entry
    
    typeMap = map
  }
  
}
