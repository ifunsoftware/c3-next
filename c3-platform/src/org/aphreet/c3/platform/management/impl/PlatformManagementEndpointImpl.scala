package org.aphreet.c3.platform.management.impl

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.config.accessor.PlatformConfigAccessor
import org.aphreet.c3.platform.storage.{StorageManager, Storage, StorageMode, StorageException}
import org.aphreet.c3.platform.storage.migration._
import org.aphreet.c3.platform.storage.dispatcher.selector.mime._
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

  var configAccessor:PlatformConfigAccessor = null
  
  var taskManager:TaskManager = null
  
  var migrationManager:MigrationManager = null
  
  var mimeSelector:MimeTypeStorageSelector = null
  
  
  private val propertyListeners:HashMap[String, Set[PlatformPropertyListener]] = new HashMap;
  
  private var currentConfig:JMap[String, String] = null;
  
  private var foundListeners:JHashSet[PlatformPropertyListener] = null;
  
  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}
  
  @Autowired
  def setPlatformConfigAccessor(accessor:PlatformConfigAccessor) = {configAccessor = accessor}
  
  @Autowired
  def setMigrationManager(manager:MigrationManager) = {migrationManager = manager}
  
  @Autowired
  def setMimeTypeStorageSelector(selector:MimeTypeStorageSelector) = {mimeSelector = selector}
  
  @Autowired{val required=false}
  def setPlatformPropertyListeners(listeners:JSet[PlatformPropertyListener]) = {
    foundListeners = new JHashSet()
    foundListeners.addAll(listeners)
  }
  
  @Autowired
  def setTaskExecutor(manager:TaskManager) = {taskManager = manager}
  
  
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
  
  def removeStorage(id:String) = {
    val storage = storageManager.storageForId(id)
    if(storage != null){
      storageManager.removeStorage(storage)
    }else{
      throw new StorageException("Can't find storage for id")
    }
  }
  
  def setStorageMode(id:String, mode:StorageMode) = storageManager.setStorageMode(id, mode)
 
  def migrateFromStorageToStorage(sourceId:String, targetId:String) = {
    migrationManager.migrateStorageToStorage(sourceId, targetId)
  }
  
  def getPlatformProperties:JMap[String, String] = {
    
    if(currentConfig == null){
      currentConfig = configAccessor.load.underlying
    }
    Collections.unmodifiableMap[String, String](currentConfig)
  }
  
  def setPlatformProperty(key:String, value:String) = {
    
	this.synchronized{

	  log info "Setting platform property: " + key
	    
	  val config = configAccessor.load

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

        configAccessor store config
      }catch{
        case e => {
          log.warn("Failed to set property " + key, e)
          throw e
        }
      }
	}
  }
  
  def listTasks:List[TaskDescription] = taskManager.taskList
  
  def setTaskMode(taskId:String, state:TaskState) ={
    state match {
      case PAUSED => taskManager.pauseTask(taskId)
      case RUNNING => taskManager.resumeTask(taskId)
      case _ => null
    }
  }
  
  def listTypeMappings:List[MimeConfigEntry] = {
    mimeSelector.configEntries
  }
  
  def addTypeMapping(mapping:MimeConfigEntry) = {
    mimeSelector.addConfigEntry(mapping)
  }
  
  def removeTypeMapping(mimeType:String) = {
    mimeSelector.removeConfigEntry(mimeType)
  }
  
  def registerPropertyListener(listener:PlatformPropertyListener) = {
    log info "Registering property listener: " + listener.getClass.getSimpleName
    
    propertyListeners.synchronized{
      for(paramName <- listener.listeningForProperties){
        propertyListeners.get(paramName) match{
          case Some(regListeners) => propertyListeners.put(paramName, regListeners + listener)
          case None => propertyListeners.put(paramName, Set(listener))
        }
        
        val currentParamValue = getPlatformProperties.get(paramName)
        
        if(currentParamValue != null)
          listener.propertyChanged(new PropertyChangeEvent(paramName, null, currentParamValue, this))
        else{
          val defaultParamValue = listener.defaultPropertyValues.get(paramName)
          if(defaultParamValue != null)
            setPlatformProperty(paramName, defaultParamValue)
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
