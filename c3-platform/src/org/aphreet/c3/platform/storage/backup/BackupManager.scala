package org.aphreet.c3.platform.storage.backup

import org.aphreet.c3.platform.common.Path

trait BackupManager {

  def backupToPath(path:Path)

}
