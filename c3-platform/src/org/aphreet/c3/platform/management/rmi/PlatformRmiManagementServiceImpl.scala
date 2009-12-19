package org.aphreet.c3.platform.management.rmi

import org.aphreet.c3.platform.exception.PlatformException
import org.aphreet.c3.platform.storage.{StorageMode, RW, USER_RO, USER_U, StorageException}
import org.aphreet.c3.platform.task.{TaskDescription, TaskState, RUNNING, PAUSED}

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import java.util.HashMap

@Component("platformRmiManagementService")
class PlatformRmiManagementServiceImpl extends PlatformRmiManagementService{

  var managementEndpoint:PlatformManagementEndpoint = null
  
  @Autowired
  def setManagementEndpoint(endPoint:PlatformManagementEndpoint) 
  	= {managementEndpoint = endPoint}
  
  def listStorages:List[StorageDescription] = {
     for(s <-managementEndpoint.listStorages)
       yield new StorageDescription(s.id, s.getClass.getSimpleName, s.path.toString, s.mode.name)
  }
   
  def listStorageTypes:List[String] = managementEndpoint.listStorageTypes
  
  def createStorage(stType:String, path:String) = managementEndpoint.createStorage(stType, path)
  
  
  def setStorageMode(id:String, mode:String) = {
    val storageMode = mode match {
      case "RW" => RW
      case "RO" => USER_RO
      case "U" => USER_U
      case _ => throw new StorageException("No mode named " + mode)
    }
    managementEndpoint.setStorageMode(id, storageMode)
  }
  
  
  def setPlatformProperty(key:String, value:String){
    managementEndpoint.setPlatformProperty(key, value)
  }
  
  def platformProperties:HashMap[String, String] = {
    
    val map = new HashMap[String, String]()
    map.putAll(managementEndpoint.getPlatformProperties)
    map
  }
  
  def listTasks:List[RmiTaskDescr] = 
    managementEndpoint.listTasks.map(fromLocalDescription(_))
  
  def fromLocalDescription(descr:TaskDescription):RmiTaskDescr = {
    new RmiTaskDescr(descr.id, descr.name, descr.state.name, descr.progress)
  }
  
  def setTaskMode(taskId:String, mode:String){
    val state:TaskState = mode match {
      case "pause" => PAUSED
      case "resume"   => RUNNING
      case _ => throw new PlatformException("mode is not valid")
      
    }
    
    managementEndpoint.setTaskMode(taskId, state)
  }
}

