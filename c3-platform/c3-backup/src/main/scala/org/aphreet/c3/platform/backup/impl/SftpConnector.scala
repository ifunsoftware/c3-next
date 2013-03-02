package org.aphreet.c3.platform.backup.impl

import com.sshtools.j2ssh.{SftpClient, SshClient}
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification
import com.sshtools.j2ssh.authentication.{AuthenticationProtocolState, PublicKeyAuthenticationClient}
import com.sshtools.j2ssh.transport.publickey.SshPrivateKeyFile
import java.io.IOException
import org.apache.commons.logging.LogFactory
import com.sshtools.j2ssh.sftp.FileAttributes


class SftpConnector(val host : String, val user : String, val privateKey : String) {
  var sshClient : SshClient = null
  var sftpClient : SftpClient = null

  val log = LogFactory getLog getClass


  def isConnected : Boolean = {
    sshClient != null && sftpClient != null &&
      sshClient.isConnected && !sftpClient.isClosed
  }

  def connect() {
    sshClient = new SshClient

    try {
      sshClient.connect(host, new IgnoreHostKeyVerification)
      log.info("Connecting to host " + host + "...")

      val authClient = new PublicKeyAuthenticationClient
      authClient.setUsername(user)

      val keyFile = SshPrivateKeyFile.parse(privateKey.getBytes)
      val key = keyFile.toPrivateKey("")
      authClient.setKey(key)

      val result = sshClient.authenticate(authClient)
      log.info("Authentication for user " + user + "...")
      if (result != AuthenticationProtocolState.COMPLETE) {
        throw new IOException("Authentication failed")
      }

      sftpClient = sshClient.openSftpClient

    } catch {
      case e: IOException => {
        log.error(e.getMessage)
      }
    }
  }

  def disconnect() {
    log.info("Disconnecting from host" + host + "...")

    if (sftpClient != null && !sftpClient.isClosed) {
      sftpClient.quit
    }

    if (sshClient != null && sshClient.isConnected) {
      sshClient.disconnect
    }

    log.info("Disconnected...")
  }

  def putFile(localFilePath : String, remoteFolder : String) {
    if (!isConnected) {
      throw new IllegalStateException("There is no connection!")
    }

    val oldDir = sftpClient.pwd()
    sftpClient.cd(remoteFolder)
    sftpClient.put(localFilePath)
    sftpClient.cd(oldDir)
  }

  def getFile(localFilePath : String, remoteFolder : String, remoteFileName : String) {
    if (!isConnected) {
      throw new IllegalStateException("There is no connection!")
    }

    val oldDir = sftpClient.pwd()
    sftpClient.cd(remoteFolder)
    sftpClient.get(remoteFileName, localFilePath)
    sftpClient.cd(oldDir)
  }

  def makeDir(path: String) {
    if (!isConnected) {
      throw new IllegalStateException("There is no connection!")
    }

    try {
      val attributes = sftpClient.stat(path)
      if (!attributes.isDirectory) {
        throw new IllegalStateException(path + " is not a directory")
      }
      log.info("Directory " + path + " already exists")

    } catch {
      case e : IOException =>  {
        log.info("Directory " + path + " is going to be created")
        sftpClient.mkdirs(path)
      }
    }
  }
}
