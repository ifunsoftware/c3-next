package org.aphreet.c3.platform.storage.migration

trait MigrationManager {

  def migrateStorageToStorage(source:Storage, target:Storage)
  
  def migrateStorageToStorage(sourceId:String, targetId:String)
  
}
