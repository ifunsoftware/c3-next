package org.aphreet.c3.platform.management.impl

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.config.PlatformConfigManager
import org.aphreet.c3.platform.storage.{StorageManager, Storage, StorageMode}
import org.aphreet.c3.platform.task._

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import scala.collection.jcl.{HashMap, HashSet}

import javax.annotation.PostConstruct
import java.util.{Map => JMap, Collections, Set => JSet, HashSet => JHashSet}

@Component("platformManagementEndpoint")
class PlatformManagementEndpointImpl extends PlatformManagementEndpoint{

  val log = LogFactory.getLog(getClass)
  
  var storageManager:StorageManager = null

  var configManager:PlatformConfigManager = null
  
  var taskExecutor:TaskExecutor = null
  
  
  private val propertyListeners:HashMap[String, Set[PlatformPropertyListener]] = new HashMap;
  
  private var currentConfig:JMap[String, String] = null;
  
  private var foundListeners:JHashSet[PlatformPropertyListener] = null;
  
  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}
  
  @Autowired
  def setPlatformConfigManager(manager:PlatformConfigManager) = {configManager = manager}
  
  @Autowired{val required=false}
  def setPlatformPropertyListeners(listeners:JSet[PlatformPropertyListener]) = {
    foundListeners = new JHashSet()
    foundListeners.addAll(listeners)
  }
  
  @Autowired
  def setTaskExecutor(executor:TaskExecutor) = {taskExecutor = executor}
  
  
  @PostConstruct
  def init{
    if(foundListeners != null){
      for(listener <- new HashSet(foundListeners)){
        this.registerPropertyListener(listener)
      }
    }
  }
  
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

	  log info "Setting platform property: " + key
	    
	  val config = configManager.getPlatformParam

	  val oldValue:String = config.get(key) match {
	    case Some(v) => v
        case None => null
	  }
	  	  
      try{
		propertyListeners.get(key) match {
		  case Some(lx) => for(l <- lx)
		  	  				 l.propertyChanged(new PropertyChangeEvent(key, oldValue, value, this))
		  case None => null
		}
    
		config.put(key, value)
		  
		if(currentConfig != null)
		  currentConfig.put(key,value)

        configManager.setPlatformParam(config)
      }catch{
        case e => {
          log.warn("Failed to set property " + key, e)
          throw e
        }
      }
	}
  }
  
  def listTasks:List[TaskDescription] = taskExecutor.taskList
  
  def registerPropertyListener(listener:PlatformPropertyListener) = {
    log info "Registering property listener: " + listener.getClass.getSimpleName
    
    propertyListeners.synchronized{
      for(paramName <- listener.listeningForProperties){
        propertyListeners.get(paramName) match{
          case Some(regListeners) => propertyListeners.put(paramName, regListeners + listener)
          case None => propertyListeners.put(paramName, Set(listener))
        }
        
        val currentParamValue = getPlatformProperties.get(paramName)
        
        if(currentParamValue != null){
          listener.propertyChanged(new PropertyChangeEvent(paramName, null, currentParamValue, this))
        }
      }
    }
  }
  
  def unregisterPropertyListener(listener:PlatformPropertyListener) = {
    log info "Unregistering property listener: " + listener.getClass.getSimpleName
    propertyListeners.synchronized{
      for(listeners <- propertyListeners.valueSet){
        listeners - listener 
      }
    }
  }
}
