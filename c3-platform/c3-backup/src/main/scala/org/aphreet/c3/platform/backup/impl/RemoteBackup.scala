package org.aphreet.c3.platform.backup.impl

import java.net.URI
import com.sshtools.j2ssh.{SftpClient, SshClient}
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification
import com.sshtools.j2ssh.authentication.{AuthenticationProtocolState, PublicKeyAuthenticationClient}
import com.sshtools.j2ssh.transport.publickey.SshPrivateKeyFile
import java.io.{IOException, File}
import org.aphreet.c3.platform.common.{Path => C3Path}
import java.nio.file._
import java.util
import org.aphreet.c3.platform.backup.{RemoteBackupLocation, AbstractBackup}

class RemoteBackup(val name: String, val create: Boolean, val config: RemoteBackupLocation) extends AbstractBackup {

  val HOST = config.host
  val USER = config.user
  val REMOTE_FOLDER = config.folder
  val PRIVATE_KEY = config.privateKey

  val sftpConnector = new SftpConnector(HOST, USER, PRIVATE_KEY)


  {
    zipFilePath = System.getProperty("java.io.tmpdir") + "/" + name
    md5FilePath = zipFilePath + ".md5"

    if (!create) {
      downloadBackup()
    }

    val env = new util.HashMap[String, String]()
    env.put("create", create.toString)
    zipFs = FileSystems.newFileSystem(URI.create("jar:file:" + zipFilePath), env, null)
  }


  def downloadBackup() {
    if (!sftpConnector.isConnected) {
      sftpConnector.connect()
    }

    sftpConnector.getFile(zipFilePath, REMOTE_FOLDER, name)
  }

  override def close() {
    super.close()

    if (!sftpConnector.isConnected) {
      sftpConnector.connect()
    }

    sftpConnector.putFile(zipFilePath, REMOTE_FOLDER)
    sftpConnector.putFile(md5FilePath, REMOTE_FOLDER)
    sftpConnector.disconnect()

    checkAndDelete(zipFilePath)
    checkAndDelete(md5FilePath)
  }

  def checkAndDelete(fileName : String) {
    val file = new File(fileName)
    if (file != null && file.exists()) {
      file.delete()
    }
  }
}


object RemoteBackup {

  def open(name: String, config: RemoteBackupLocation): RemoteBackup = {
    new RemoteBackup(name, false, config)
  }

  def create(name: String, config: RemoteBackupLocation): RemoteBackup = {
    new RemoteBackup(name, true, config)
  }

  def directoryForAddress(fs: FileSystem, address: String): Path = {
    val firstLetter = address.charAt(0).toString
    val secondLetter = address.charAt(1).toString
    val thirdLetter = address.charAt(2).toString

    fs.getPath(firstLetter, secondLetter, thirdLetter)
  }
}
