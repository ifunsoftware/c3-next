package org.aphreet.c3.platform.storage

import dispatcher.StorageDispatcher
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.common.Path

trait StorageManager {

  def registerFactory(factory:StorageFactory)
  
  def unregisterFactory(factory:StorageFactory)
  
  def storageForId(id:String):Storage

  def storageForResource(resource:Resource):Storage

  def createStorage(storageType:String, storagePath:Path)
  
  def listStorages:List[Storage]
  
  def removeStorage(storage:Storage)
  
  def listStorageTypes:List[String]
    
  def setStorageMode(id:String, mode:StorageMode)
  
  def updateStorageParams(storage:Storage)

  def createIndex(id:String, index:StorageIndex)

  def removeIndex(id:String, name:String)

  def addSecondaryId(id:String, secondaryId:String)
}
