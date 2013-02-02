package org.aphreet.c3.platform.backup.impl

import java.net.URI
import com.sshtools.j2ssh.{SftpClient, SshClient}
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification
import com.sshtools.j2ssh.authentication.{AuthenticationProtocolState, PublicKeyAuthenticationClient}
import com.sshtools.j2ssh.transport.publickey.{SshPrivateKey, SshPrivateKeyFile}
import java.io.{IOException, File}
import org.aphreet.c3.platform.common.{Path => C3Path}
import java.nio.file._
import java.util
import org.aphreet.c3.platform.backup.AbstractBackup

/**
 * Created with IntelliJ IDEA.
 * User: antey
 * Date: 01.02.13
 * Time: 23:55
 * To change this template use File | Settings | File Templates.
 */
class RemoteBackup(val uri:URI, val create:Boolean) extends AbstractBackup {

  var HOST : String = null
  var USER : String = null
  var PRIVATE_KEY_FILE_NAME : String = null

  var remotePath : String = null
  var backupName : String = null
  var tempBackupPath : String = null

  {
      initZip()
  }

  def initZip() {
    HOST = "backup-c3backup.rhcloud.com"
    USER = "d22b442f096243d499120ff44adfc76a"
    PRIVATE_KEY_FILE_NAME = "/home/antey/.ssh/id_rsa"

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
    val sshClient: SshClient = new SshClient

    try {
      sshClient.connect(HOST, new IgnoreHostKeyVerification)

      val authClient: PublicKeyAuthenticationClient = new PublicKeyAuthenticationClient
      authClient.setUsername(USER)

      val keyFile: SshPrivateKeyFile = SshPrivateKeyFile.parse(new File(PRIVATE_KEY_FILE_NAME))
      val key: SshPrivateKey = keyFile.toPrivateKey("")
      authClient.setKey(key)

      val result: Int = sshClient.authenticate(authClient)
      if (result != AuthenticationProtocolState.COMPLETE) {
        throw new IOException("Fail")
      }

      val sftpClient: SftpClient = sshClient.openSftpClient
      System.out.println("PWD: " + sftpClient.pwd)
      sftpClient.cd(remotePath)

      System.out.println(tempBackupPath)

      sftpClient.get(backupName, tempBackupPath)

      sftpClient.quit
      sshClient.disconnect
    }
    catch {
      case e: IOException => {
        e.printStackTrace
      }
    }
  }

  override def close() {
    super.close()

    val sshClient: SshClient = new SshClient

    try {
      sshClient.connect(HOST, new IgnoreHostKeyVerification)

      val authClient: PublicKeyAuthenticationClient = new PublicKeyAuthenticationClient
      authClient.setUsername(USER)

      val keyFile: SshPrivateKeyFile = SshPrivateKeyFile.parse(new File(PRIVATE_KEY_FILE_NAME))
      val key: SshPrivateKey = keyFile.toPrivateKey("")
      authClient.setKey(key)

      val result: Int = sshClient.authenticate(authClient)
      if (result != AuthenticationProtocolState.COMPLETE) {
        throw new IOException("Fail")
      }

      val sftpClient: SftpClient = sshClient.openSftpClient
      System.out.println("PWD: " + sftpClient.pwd)
      sftpClient.cd(remotePath)

      System.out.println(tempBackupPath)

      sftpClient.put(tempBackupPath)

      sftpClient.quit
      sshClient.disconnect
    }
    catch {
      case e: IOException => {
        e.printStackTrace
      }
    }

    val localBackup : File = new File(tempBackupPath)
    localBackup.delete()
  }
}


object RemoteBackup {

  def open(path:C3Path):RemoteBackup = {
    val zipFile = URI.create(path.toString)
    new RemoteBackup(zipFile, false)
  }

  def create(path:C3Path):RemoteBackup = {

    val zipFile = URI.create(path.toString)

    new RemoteBackup(zipFile, true)
  }

  def directoryForAddress(fs:FileSystem, address:String):Path = {
    val firstLetter = address.charAt(0).toString
    val secondLetter = address.charAt(1).toString
    val thirdLetter = address.charAt(2).toString

    fs.getPath(firstLetter, secondLetter, thirdLetter)
  }

}
