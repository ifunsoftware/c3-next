package org.aphreet.c3.platform.storage.backup.impl

import org.aphreet.c3.platform.common.Path

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component
class BackupManagerImpl extends BackupManager{

  var storageManager:StorageManager = null
  
  def setStorageManager(manager:StorageManager) = {storageManager = manager}
  
  def backupToPath(path:Path) = {
    
  }
}
