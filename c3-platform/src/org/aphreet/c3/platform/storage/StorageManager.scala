package org.aphreet.c3.platform.storage

import java.io.OutputStream

import dispatcher.StorageDispatcher
import org.aphreet.c3.platform.resource.{Resource, DataWrapper}
import org.aphreet.c3.platform.common.Path

trait StorageManager {

  def registerFactory(factory:StorageFactory)
  
  def unregisterFactory(factory:StorageFactory)
  
  def storageForId(id:String):Storage
  
  def createStorage(storageType:String, storagePath:Path)
  
  def listStorages:List[Storage]
  
  def removeStorage(storage:Storage)
  
  def listStorageTypes:List[String]
  
  def dispatcher:StorageDispatcher
  
  def setStorageMode(id:String, mode:StorageMode)
  
  def updateStorageParams(storage:Storage)
  
}
