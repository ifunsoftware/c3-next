package org.aphreet.c3.platform.backup

import ssh.SftpConnector
import scala.beans.BeanProperty
import java.io.File
import org.aphreet.c3.platform.common.Logger

case class BackupLocation(
            @BeanProperty var id: String,
            @BeanProperty var backupType: String,
            @BeanProperty var host: String,
            @BeanProperty var user: String,
            @BeanProperty var folder: String,
            @BeanProperty var privateKey: String,
            @BeanProperty var schedule: List[String]
        ) extends java.io.Serializable {

  def this() = this(null, null, null, null, null, null, null)
}


object LocalBackupLocation {

  val log = Logger(getClass)

  def create(id: String, path: String) : BackupLocation =  {

    val folder = new File(path)
    if (folder.exists()) {
      if (!folder.isDirectory) {
        throw new IllegalStateException("Path is not a folder")
      }
    } else if (folder.mkdirs()) {
      log.info("Diretory " + path + " was created")
    } else {
      throw new IllegalStateException("Folder creation failed")
    }

    new BackupLocation(id, "local", "", "", path, "", null)
  }
}


object RemoteBackupLocation {

  val log = Logger(getClass)

  def create(id: String, host: String, user: String, path: String, privateKeyFile: String) : BackupLocation=  {

    val file = new File(privateKeyFile)

    if (!file.exists()) {
      throw new IllegalArgumentException("Private key file doesn't exist")
    }
    if (!file.isFile) {
      throw new IllegalArgumentException("Private key file isn't a real file")
    }

    val connector = new SftpConnector(host, user, privateKeyFile)
    try {
      connector.connect()
      if (!connector.isConnected) {
        throw new IllegalStateException("Connection failed! Target isn't going to be created.")
      }

      connector.makeDir(path)
      log.info("Diretory " + path + " was created")
    } finally {
      connector.disconnect()
    }

    new BackupLocation(id, "remote", host, user, path, privateKeyFile, Nil)
  }
}