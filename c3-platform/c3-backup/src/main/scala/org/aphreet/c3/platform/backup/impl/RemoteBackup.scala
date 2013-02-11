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

class RemoteBackup(val name : String, val create: Boolean, val config: RemoteBackupLocation) extends AbstractBackup {

  val HOST = config.host
  val USER = config.user
  val REMOTE_FOLDER = config.folder
  val PRIVATE_KEY = config.privateKey

  var tempBackupPath: String = null

  {
    initZip()
  }

  def initZip() {
    tempBackupPath = System.getProperty("java.io.tmpdir") + "/" + name

    if (!create) {
      downloadBackup()
    }

    val env = new util.HashMap[String, String]()
    env.put("create", create.toString)
    zipFs = FileSystems.newFileSystem(URI.create("jar:file:" + tempBackupPath), env, null)
  }

  def downloadBackup() {
    val sshClient = new SshClient
    var sftpClient: SftpClient = null

    try {
      sshClient.connect(HOST, new IgnoreHostKeyVerification)
      log.info("Connecting to host " + HOST + "...")

      val authClient = new PublicKeyAuthenticationClient
      authClient.setUsername(USER)

      val keyFile = SshPrivateKeyFile.parse(PRIVATE_KEY.getBytes)
      val key = keyFile.toPrivateKey("")
      authClient.setKey(key)

      val result = sshClient.authenticate(authClient)
      log.info("Authentication for user " + USER + "...")
      if (result != AuthenticationProtocolState.COMPLETE) {
        throw new IOException("Authentication failed")
      }

      sftpClient = sshClient.openSftpClient
      sftpClient.cd(REMOTE_FOLDER)

      sftpClient.get(name, tempBackupPath)

    } catch {
      case e: IOException => {
        log.error(e.getMessage)
      }

    } finally {
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
    var sftpClient: SftpClient = null

    try {
      sshClient.connect(HOST, new IgnoreHostKeyVerification)
      log.info("Connecting to host " + HOST + "...")

      val authClient = new PublicKeyAuthenticationClient
      authClient.setUsername(USER)

      val keyFile = SshPrivateKeyFile.parse(PRIVATE_KEY.getBytes)
      val key = keyFile.toPrivateKey("")
      authClient.setKey(key)

      val result = sshClient.authenticate(authClient)
      log.info("Authentication for user " + USER + "...")
      if (result != AuthenticationProtocolState.COMPLETE) {
        throw new IOException("Authentication failed")
      }

      sftpClient = sshClient.openSftpClient
      sftpClient.cd(REMOTE_FOLDER)

      sftpClient.put(tempBackupPath)

    } catch {
      case e: IOException => {
        log.error(e.getMessage)
      }

    } finally {
      if (sftpClient != null && !sftpClient.isClosed) {
        sftpClient.quit
      }
      if (sshClient != null && sshClient.isConnected) {
        sshClient.disconnect
      }
    }

    val localBackup = new File(tempBackupPath)
    if (localBackup != null && localBackup.exists()) {
      localBackup.delete()
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
