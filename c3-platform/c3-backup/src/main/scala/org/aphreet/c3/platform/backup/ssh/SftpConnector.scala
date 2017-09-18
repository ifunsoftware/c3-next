package org.aphreet.c3.platform.backup.ssh

import collection.mutable.ListBuffer
import com.sshtools.j2ssh.authentication.{PasswordAuthenticationClient, SshAuthenticationClient, AuthenticationProtocolState, PublicKeyAuthenticationClient}
import com.sshtools.j2ssh.session.SessionChannelClient
import com.sshtools.j2ssh.sftp.SftpFile
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification
import com.sshtools.j2ssh.transport.publickey.SshPrivateKeyFile
import com.sshtools.j2ssh.{FileTransferProgress, SshException, SftpClient, SshClient}
import java.io._
import java.lang.String
import org.apache.commons.codec.digest.DigestUtils
import org.aphreet.c3.platform.common.Disposable._
import org.aphreet.c3.platform.common.Logger
import scala.Predef._
import scala.collection.JavaConversions._


class SftpConnector(val host: String, val user: String, val privateKey: String, val port: Int = 22) {

  var sshClient: SshClient = null
  var sftpClient: SftpClient = null

  val MD5_CMD_TEMPLATE = "cd \"%s\" && md5sum \"%s\" && cd -"
  val CMD_NOT_FOUND = "command not found"
  val BAD_ARG = "No such file or directory"

  val log = Logger(getClass)


  def isConnected: Boolean = {
    sshClient != null && sftpClient != null &&
      sshClient.isConnected && !sftpClient.isClosed
  }

  def connect() {
    try {
      val authClient = new PublicKeyAuthenticationClient
      authClient.setUsername(user)

      val keyFile = SshPrivateKeyFile.parse(privateKey.getBytes)
      val key = keyFile.toPrivateKey("")
      authClient.setKey(key)

      connect(authClient)

    } catch {
      case e: IOException => {
        log.error(e.getMessage)
      }
    }
  }

  def connect(password: String) {
    try {
      val authClient = new PasswordAuthenticationClient
      authClient.setUsername(user)
      authClient.setPassword(password)

      connect(authClient)

    } catch {
      case e: IOException => {
        log.error(e.getMessage)
      }
    }
  }

  private def connect(authClient: SshAuthenticationClient) {
    sshClient = new SshClient

    if (port < 0) {
      sshClient.connect(host, new IgnoreHostKeyVerification)
      log.info("Connecting to host " + host + "...")
    } else {
      sshClient.connect(host, port, new IgnoreHostKeyVerification)
      log.info("Connecting to host " + host + " on port " + port + "...")
    }


    val result = sshClient.authenticate(authClient)
    log.info("Authentication for user " + user + "...")
    if (result != AuthenticationProtocolState.COMPLETE) {
      throw new SshException("Authentication failed")
    }

    sftpClient = sshClient.openSftpClient
  }

  def disconnect() {
    log.info("Disconnecting from host" + host + "...")

    if (sftpClient != null && !sftpClient.isClosed) {
      sftpClient.quit()
    }

    if (sshClient != null && sshClient.isConnected) {
      sshClient.disconnect()
    }

    log.info("Disconnected...")
  }

  def putFile(localFilePath: String, remoteFolder: String) {
    if (!isConnected) {
      throw new IllegalStateException("There is no connection!")
    }

    val oldDir = sftpClient.pwd()
    sftpClient.cd(remoteFolder)

    sftpClient.put(localFilePath, new FileTransferProgress {
      def isCancelled = false

      def progressed(progress: Long) {
        log.info("{} bytes has been written", progress)
      }

      def completed() {
        log.info("File uploaded")
      }

      def started(total: Long, path: String) {
        log.info("Going to write {} bytes to {}", total, path)
      }
    })

    sftpClient.put(localFilePath)
    sftpClient.cd(oldDir)
  }

  def getFile(localFilePath: String, remoteFolder: String, remoteFileName: String) {
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
      case e: IOException => {
        log.info("Directory " + path + " is going to be created")
        sftpClient.mkdirs(path)
      }
    }
  }

  def listFiles(folder: String): List[String] = {
    if (!isConnected) {
      throw new IllegalStateException("There is no connection!")
    }

    val fileNames = new ListBuffer[String]()

    sftpClient.ls(folder).toList
      .filter(file => file.asInstanceOf[SftpFile].isFile)
      .foreach(file => fileNames += file.asInstanceOf[SftpFile].getFilename)

    fileNames.toList
  }

  def getMd5Hash(folder: String, fileName: String): String = {
    if (!isConnected) {
      throw new IllegalStateException("There is no connection!")
    }
    var md5Hash = ""

    var session: SessionChannelClient = null
    try {
      session = sshClient.openSessionChannel()
      // really wide terminal, it has enough space for every command
      // TODO improve parser
      if (session.requestPseudoTerminal("ansi", 1024, 24, 0, 0, "")) {
        session.startShell
        val out = session.getOutputStream

        using(new BufferedReader(new InputStreamReader(session.getInputStream)))(in => {
          val cmd = String.format(MD5_CMD_TEMPLATE, folder, fileName)
          out.write((cmd + "\n").getBytes)

          var line: String = null
          do {
            line = in.readLine()
          } while (!line.endsWith(cmd) || line.length == cmd.length)
          line = in.readLine()
          if (!line.equals(CMD_NOT_FOUND) && !line.equals(BAD_ARG)) {
            md5Hash = line.split("\\s+")(0)
          } else {
            log.info("Calculating MD5 sum on remote host failed")
          }
        })
      }

    } finally {
      if (session != null && session.isOpen) {
        session.close()
      }
    }

    md5Hash
  }

  def hasValidChecksum(folder: String, fileName: String): Boolean = {
    log.info("Verifying MD5 sum for file " + folder + "/" + fileName)

    var isValid = false
    val tempFilePath: String = System.getProperty("java.io.tmpdir") + '/' + fileName
    var backupFile = new File(tempFilePath)

    val calculatedMd5 =
      if (backupFile.exists() && backupFile.isFile) {
        log.info("File " + fileName + " was previously cached in temporary directory")
        using(new FileInputStream(backupFile))(is => DigestUtils.md5Hex(is))

      } else {
        log.info("Calculating MD5 sum on remote host...")
        var md5 = ""

        try {
          md5 = getMd5Hash(folder, fileName)
        } catch {
          case e: Exception => log.warn("Failed to calculate md5 of backup ", e)
        }
        if (md5.equals("")) {
          log.info("Downloading file " + fileName + " to the local temporary directory...")

          getFile(tempFilePath, folder, fileName)
          backupFile = new File(tempFilePath)
          if (backupFile.exists() && backupFile.isFile) {
            using(new FileInputStream(backupFile))(is => md5 = DigestUtils.md5Hex(is))
          }
        }
        md5
      }

    getFile(tempFilePath + ".md5", folder, fileName + ".md5")
    val md5File = new File(tempFilePath + ".md5")
    if (md5File.exists() && md5File.isFile) {
      using(new BufferedReader(new FileReader(md5File)))(reader => {
        isValid = reader.readLine().equals(calculatedMd5)
      })
    } else {
      log.info("File " + md5File.getName + " doesn't exist")
    }

    isValid
  }

  def close() {
    disconnect()
  }
}
