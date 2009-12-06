package org.aphreet.c3.platform.management.impl

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.config.PlatformConfigManager
import org.aphreet.c3.platform.storage.{StorageManager, Storage, StorageMode}

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import scala.collection.jcl.HashMap

import java.util.{Map => JMap, Collections}

@Component("platformManagementEndpoint")
class PlatformManagementEndpointImpl extends PlatformManagementEndpoint{

  val log = LogFactory.getLog(getClass)
  
  var storageManager:StorageManager = null

  var configManager:PlatformConfigManager = null
  
  private var currentConfig:JMap[String, String] = null;
  
  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}
  
  @Autowired
  def setPlatformConfigManager(manager:PlatformConfigManager) = {configManager = manager}
  
  
  def listStorages:List[Storage] = storageManager.listStorages
  
  def listStorageTypes:List[String] = storageManager.listStorageTypes
  
  def createStorage(storageType:String, path:String) = storageManager.createStorage(storageType, new Path(path))
  
  def setStorageMode(id:String, mode:StorageMode) = storageManager.setStorageMode(id, mode)
 
  def getPlatformProperties:JMap[String, String] = {
    
    if(currentConfig == null){
      currentConfig = configManager.getPlatformParam.underlying
    }
    Collections.unmodifiableMap[String, String](currentConfig)
  }
  
  def setPlatformProperty(key:String, value:String) = {
    
    
	  this.synchronized{

		  val config = configManager.getPlatformParam

		  config.put(key, value)
		  
		  if(currentConfig != null){
			  currentConfig.put(key,value)
		  }

		  configManager.setPlatformParam(config)
	  }
  }
}
