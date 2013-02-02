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

class RemoteBackup(val uri:URI, val create:Boolean, val config : RemoteBackupLocation) extends AbstractBackup {

  var HOST = config.host
  var USER = config.user
  var PRIVATE_KEY_FILE_NAME = config.privateKeyLocation

  var remotePath : String = null
  var backupName : String = null
  var tempBackupPath : String = null

  {
    initZip()
  }

  def initZip() {
    val stringUri = uri.toString
    val lastSlash = stringUri.lastIndexOf("/")
    remotePath = stringUri.substring(0, lastSlash)
    backupName = stringUri.substring(lastSlash + 1)
    tempBackupPath = System.getProperty("java.io.tmpdir") + "/" + backupName

    if (!create) {
      downloadBackup()
    }

    val env = new util.HashMap[String, String]()
    env.put("create", create.toString)
    zipFs = FileSystems.newFileSystem(URI.create("jar:file:" + tempBackupPath), env, null)
  }

  def downloadBackup() {
    val sshClient = new SshClient
    var sftpClient : SftpClient = null

    try {
      sshClient.connect(HOST, new IgnoreHostKeyVerification)
      log.info("Connecting to host " + HOST + "...")

      val authClient = new PublicKeyAuthenticationClient
      authClient.setUsername(USER)

      val keyFile = SshPrivateKeyFile.parse(new File(PRIVATE_KEY_FILE_NAME))
      val key = keyFile.toPrivateKey("")
      authClient.setKey(key)

      val result = sshClient.authenticate(authClient)
      log.info("Authentication for user " + USER + "...")
      if (result != AuthenticationProtocolState.COMPLETE) {
        throw new IOException("Authentication failed")
      }

      sftpClient = sshClient.openSftpClient
      sftpClient.cd(remotePath)

      sftpClient.get(backupName, tempBackupPath)

    } catch {
      case e: IOException => {
        log.error(e.getMessage)
      }

    } finally  {
      if (sftpClient != null && !sftpClient.isClosed) {
        sftpClient.quit
      }
      if (sshClient != null && sshClient.isConnected) {
        sshClient.disconnect
      }
    }
  }

  override def close() {
    super.close()

    val sshClient = new SshClient
    var sftpClient : SftpClient = null

    try {
      sshClient.connect(HOST, new IgnoreHostKeyVerification)
      log.info("Connecting to host " + HOST + "...")

      val authClient = new PublicKeyAuthenticationClient
      authClient.setUsername(USER)

      val keyFile = SshPrivateKeyFile.parse(new File(PRIVATE_KEY_FILE_NAME))
      val key = keyFile.toPrivateKey("")
      authClient.setKey(key)

      val result = sshClient.authenticate(authClient)
      log.info("Authentication for user " + USER + "...")
      if (result != AuthenticationProtocolState.COMPLETE) {
        throw new IOException("Authentication failed")
      }

      sftpClient = sshClient.openSftpClient
      sftpClient.cd(remotePath)

      sftpClient.put(tempBackupPath)

    } catch {
      case e: IOException => {
        log.error(e.getMessage)
      }

    } finally  {
      if (sftpClient != null && !sftpClient.isClosed) {
        sftpClient.quit
      }
      if (sshClient != null && sshClient.isConnected) {
        sshClient.disconnect
      }
    }

    val localBackup = new File(tempBackupPath)
    localBackup.delete()
  }
}


object RemoteBackup {

  def open(path:C3Path, config:RemoteBackupLocation):RemoteBackup = {
    val zipFile = URI.create(path.toString)
    new RemoteBackup(zipFile, false, config)
  }

  def create(path:C3Path, config:RemoteBackupLocation):RemoteBackup = {

    val zipFile = URI.create(path.toString)

    new RemoteBackup(zipFile, true, config)
  }

  def directoryForAddress(fs:FileSystem, address:String):Path = {
    val firstLetter = address.charAt(0).toString
    val secondLetter = address.charAt(1).toString
    val thirdLetter = address.charAt(2).toString

    fs.getPath(firstLetter, secondLetter, thirdLetter)
  }
}
