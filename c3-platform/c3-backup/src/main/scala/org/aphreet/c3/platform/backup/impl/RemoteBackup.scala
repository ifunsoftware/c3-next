package org.aphreet.c3.platform.backup.impl

import java.net.URI
import java.io.{FileReader, BufferedReader, FileInputStream, File}
import org.aphreet.c3.platform.common.{Path => C3Path}
import java.nio.file._
import java.util
import org.aphreet.c3.platform.backup.{BackupLocation, AbstractBackup}
import org.apache.commons.codec.digest.DigestUtils
import org.aphreet.c3.platform.common.Disposable._
import io.Source
import org.apache.commons.logging.LogFactory

class RemoteBackup(val name: String, val create: Boolean, val config: BackupLocation) extends AbstractBackup {

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

  val log = LogFactory getLog getClass

  def open(name: String, config: BackupLocation): RemoteBackup = {
    new RemoteBackup(name, false, config)
  }

  def create(name: String, config: BackupLocation): RemoteBackup = {
    new RemoteBackup(name, true, config)
  }

  def directoryForAddress(fs: FileSystem, address: String): Path = {
    val firstLetter = address.charAt(0).toString
    val secondLetter = address.charAt(1).toString
    val thirdLetter = address.charAt(2).toString

    fs.getPath(firstLetter, secondLetter, thirdLetter)
  }

  def hasValidChecksum(connector: SftpConnector, folder: String, fileName: String) : Boolean = {
    log.info("Verifying MD5 sum for file " + folder + "/" + fileName)

    var isValid = false
    val tempFilePath: String = System.getProperty("java.io.tmpdir") + '/' + fileName
    var backupFile = new File(tempFilePath)

    val calculatedMd5 =
      if (backupFile.exists() && backupFile.isFile) {
        log.info("File " + fileName + " was previously cached in temporary directory")
        using(new FileInputStream(backupFile)) (is => DigestUtils.md5Hex(is))

      } else {
        log.info("Calculating MD5 sum on remote host...")
        var md5 = connector.getMd5Hash(folder, fileName)

        if (md5.equals("")) {
          log.info("Downloading file " + fileName + " to the local temporary directory...")

          connector.getFile(tempFilePath, folder, fileName)
          backupFile = new File(tempFilePath)
          if (backupFile.exists() && backupFile.isFile) {
            using(new FileInputStream(backupFile)) (is => md5 = DigestUtils.md5Hex(is))
          }
        }
        md5
      }

    connector.getFile(tempFilePath + ".md5", folder, fileName + ".md5")
    val md5File = new File(tempFilePath + ".md5")
    if (md5File.exists() && md5File.isFile) {
      using (new BufferedReader(new FileReader(md5File))) (reader => {
        isValid = reader.readLine().equals(calculatedMd5)
      })
    } else {
      log.info("File " + md5File.getName + " doesn't exist")
    }

    isValid
  }
}
