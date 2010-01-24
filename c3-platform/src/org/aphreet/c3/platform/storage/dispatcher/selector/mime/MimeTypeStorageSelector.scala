package org.aphreet.c3.platform.storage.dispatcher.selector.mime

import scala.collection.mutable.{HashMap, ArrayBuffer}

import eu.medsea.mimeutil.MimeType

class MimeTypeStorageSelector {

  private var typeMap = new HashMap[String, TypeMapping]
  
  var configAccessor:MimeTypeConfigAccessor = null
  
  def setConfigAccessor(accessor:MimeTypeConfigAccessor) = {configAccessor = accessor}
  
  
  def defaultStorageType:(String,Boolean) = null
  
  def storageForType(mime:MimeType):(String, Boolean) = {
    
    typeMap.get(mime.getMediaType) match {
      case Some(subtypes) => subtypes storageForSubtype mime.getSubType match {
        case Some(st) => st
        case None => defaultStorageType
      }
      case None => defaultStorageType
    }
  }
  
  def addConfigEntry(entry:MimeConfigEntry) = {
    configAccessor.update(entries => entry :: configEntries.filter(_.mimeType != entry.mimeType))
  }
  
  def removeConfigEntry(mimeType:String) = {
    configAccessor.update(entries => configEntries.filter(_.mimeType != mimeType))
  }
  
  def configEntries:List[MimeConfigEntry] = {
    
    val entries = new ArrayBuffer[MimeConfigEntry]
    
    for((mimeType, mapping) <- typeMap)
      entries ++ mapping.configuration(mimeType)
    
    if(defaultStorageType != null)
      entries + MimeConfigEntry("*/*", defaultStorageType._1, defaultStorageType._2)
    
    entries.toList
  }
}

private class TypeMapping{
  
  private var subTypeMap = new HashMap[String, (String, Boolean)]
 
  var defaultStorageType:(String, Boolean) = null

  def storageForSubtype(subtype:String):Option[(String,Boolean)] = 
    subTypeMap.get(subtype) match {
      case Some(s) => Some(s)
      case None => {
        if(defaultStorageType != null)
          Some(defaultStorageType)
        else
          None
      }
    }
  
  def configuration(mainType:String):List[MimeConfigEntry] = {
    
    val buffer = new ArrayBuffer[MimeConfigEntry]
    
    for((subType, (storage, versioned)) <- subTypeMap){
      
      val mimeType = mainType + "/" + subType
      
      buffer + MimeConfigEntry(mimeType, storage, versioned)
    }
    
    if(defaultStorageType != null){
      buffer + MimeConfigEntry(mainType + "/*", defaultStorageType._1, defaultStorageType._2)
    }
    
    buffer.toList
    
  }
}