package org.aphreet.c3.platform.storage.migration

import org.aphreet.c3.platform.storage.Storage

trait MigrationManager {

  def migrateStorageToStorage(source:Storage, target:Storage)
  
  def migrateStorageToStorage(sourceId:String, targetId:String)
  
}
