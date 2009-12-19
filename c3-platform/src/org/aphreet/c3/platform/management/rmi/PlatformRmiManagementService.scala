package org.aphreet.c3.platform.management.rmi

import java.util.HashMap

trait PlatformRmiManagementService {

  def listStorages:List[StorageDescription]
  
  def listStorageTypes:List[String]
  
  def createStorage(stType:String, path:String)
  
  def setStorageMode(id:String, mode:String)
  
  def setPlatformProperty(key:String, value:String)
  
  def platformProperties:HashMap[String, String]
  
  def listTasks:List[RmiTaskDescr]
  
  def setTaskMode(taskId:String, mode:String)
  
  
}
