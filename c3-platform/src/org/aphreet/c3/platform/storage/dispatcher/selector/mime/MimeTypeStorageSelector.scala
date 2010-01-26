package org.aphreet.c3.platform.storage.dispatcher.selector.mime

import scala.collection.mutable.{HashMap, ArrayBuffer}

import eu.medsea.mimeutil.MimeType

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.PostConstruct

@Component
class MimeTypeStorageSelector {

  private var typeMap = new HashMap[String, MimeConfigEntry]
  
  var configAccessor:MimeTypeConfigAccessor = null

  @Autowired
  def setConfigAccessor(accessor:MimeTypeConfigAccessor) = {configAccessor = accessor}
  
  @PostConstruct
  def init{
    for(entry <- configAccessor.load)
      typeMap.put(entry.mimeType, entry)
  }
  
  def storageForType(mime:MimeType):MimeConfigEntry = {
    
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
  
  def addConfigEntry(entry:MimeConfigEntry) = {
    typeMap.put(entry.mimeType, entry)
    configAccessor.update(entries => entry :: configEntries.filter(_.mimeType != entry.mimeType))
  }
  
  def removeConfigEntry(mimeType:String) = {
    typeMap - mimeType
    configAccessor.update(entries => configEntries.filter(_.mimeType != mimeType))
  }
  
  def configEntries:List[MimeConfigEntry] = typeMap.values.toList
  
}
