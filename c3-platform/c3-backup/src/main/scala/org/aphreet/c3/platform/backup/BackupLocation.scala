package org.aphreet.c3.platform.backup

import ssh.SftpConnector
import java.io.{IOException, File}
import org.aphreet.c3.platform.common.{Disposable, Logger}
import org.aphreet.c3.platform.backup.impl.{LocalBackup, RemoteBackup}

sealed trait BackupLocation {

  def id: String

  def schedule: List[String]

  def schedule_=(schedule: List[String])

  def createBackup(name: String): AbstractBackup

  def openBackup(name: String): AbstractBackup

  def listBackups: List[String]

  def typeAlias: String
}

case class LocalBackupLocation(
                                var id: String,
                                var directory: String,
                                var schedule: List[String]) extends BackupLocation {

  def createBackup(name: String): AbstractBackup =
    new LocalBackup(new File(directory, name), true)

  def openBackup(name: String): AbstractBackup =
    new LocalBackup(new File(directory, name), false)

  def listBackups: List[String] = ???

  def typeAlias: String = "local"
}

case class RemoteBackupLocation(
                                 var id: String,
                                 var host: String,
                                 var port: Int = 22,
                                 var user: String,
                                 var folder: String,
                                 var privateKey: String,
                                 var password: String,
                                 var schedule: List[String]) extends BackupLocation {

  def createBackup(name: String): AbstractBackup = new RemoteBackup(name, true, this)

  def openBackup(name: String): AbstractBackup = new RemoteBackup(name, false, this)

  def listBackups: List[String] = RemoteBackup.listBackups(this)

  def typeAlias: String = "remote"
}


object LocalBackupLocation {

  val log = Logger(getClass)

  def create(id: String, path: String): BackupLocation = {

    val folder = new File(path)

    if (folder.exists()) {
      if (!folder.isDirectory) {
        throw new IllegalArgumentException("Path is not a folder")
      }
    }

    folder.mkdirs()

    LocalBackupLocation(id, path, Nil)
  }
}


object RemoteBackupLocation {

  val log = Logger(getClass)

  def create(id: String, host: String, port: Int, user: String, path: String,
             privateKeyFile: String, password: String): BackupLocation = {

    val file = new File(privateKeyFile)

    if (!file.exists()) {
      throw new IllegalArgumentException("Private key file doesn't exist")
    }
    if (!file.isFile) {
      throw new IllegalArgumentException("Private key file isn't a real file")
    }

    Disposable.using(new SftpConnector(host, user, privateKeyFile))(connector => {
      if (!connector.isConnected) {
        throw new IOException("Connection failed! Target isn't going to be created.")
      }

      connector.makeDir(path)
      log.info("Directory " + path + " was created")
    })

    RemoteBackupLocation(id, host, port, user, path, privateKeyFile, password, Nil)
  }
}