package org.aphreet.c3.platform.remote.rmi.management

import java.util.HashMap

trait PlatformRmiManagementService {

  def listStorages:List[StorageDescription]
  
  def listStorageTypes:List[String]
  
  def createStorage(stType:String, path:String)
  
  def removeStorage(id:String)
  
  def migrate(source:String, target:String)
  
  def setStorageMode(id:String, mode:String)
  
  def setPlatformProperty(key:String, value:String)
  
  def platformProperties:HashMap[String, String]
  
  def listTasks:List[RmiTaskDescr]
  
  def setTaskMode(taskId:String, mode:String)
  
  def listTypeMappigs:List[RmiMimeTypeMapping]
  
  def addTypeMapping(mimeType:String, storage:String, versioned:java.lang.Short)
  
  def removeTypeMapping(mimeType:String)
  
  def listSizeMappings:List[RmiSizeMapping]
  
  def addSizeMapping(size:java.lang.Long, storage:String, versioned:java.lang.Integer)
  
  def removeSizeMapping(size:java.lang.Long)

}
