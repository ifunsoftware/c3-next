package org.aphreet.c3.platform.management

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.storage.{StorageManager, Storage, StorageMode}

import scala.collection.mutable.HashMap

trait PlatformManagementEndpoint {
  
  def listStorages:List[Storage]
  
  def listStorageTypes:List[String]
  
  def createStorage(storageType:String, path:String)
  
  def setStorageMode(id:String, mode:StorageMode)
  
  def getPlatformProperties:HashMap[String, String]
  
  def setPlatformProperty(key:String, value:String)

}
