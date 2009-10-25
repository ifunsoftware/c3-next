package org.aphreet.c3.platform.management.impl

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.config.PlatformConfigManager

import org.aphreet.c3.platform.storage.{StorageManager, Storage, StorageMode}

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import scala.collection.mutable.HashMap

@Component("platformManagementEndpoint")
class PlatformManagementEndpointImpl extends PlatformManagementEndpoint{

  val log = LogFactory.getLog(getClass)
  
  var storageManager:StorageManager = null

  var configManager:PlatformConfigManager = null
  
  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}
  
  @Autowired
  def setPlatformConfigManager(manager:PlatformConfigManager) = {configManager = manager}
  
  
  def listStorages:List[Storage] = storageManager.listStorages
  
  def listStorageTypes:List[String] = storageManager.listStorageTypes
  
  def createStorage(storageType:String, path:String) = storageManager.createStorage(storageType, path)
  
  def setStorageMode(id:String, mode:StorageMode) = storageManager.setStorageMode(id, mode)
 
  def getPlatformProperties:HashMap[String, String] = configManager.getPlatformParam
  
  def setPlatformProperty(key:String, value:String) = {
    val config = configManager.getPlatformParam
    
    config.put(key, value)
    
    configManager.setPlatformParam(config)
  }
}
