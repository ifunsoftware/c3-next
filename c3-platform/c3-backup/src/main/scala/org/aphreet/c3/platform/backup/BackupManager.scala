package org.aphreet.c3.platform.backup

trait BackupManager {

  def createBackup()

  def restoreBackup(location:String)

}
