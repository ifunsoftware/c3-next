package org.aphreet.c3.platform.backup.impl

import java.io.{FileReader, BufferedReader, FileInputStream, File}
import java.net.URI
import java.nio.file._
import java.util
import org.apache.commons.codec.digest.DigestUtils
import org.aphreet.c3.platform.backup.ssh.SftpConnector
import org.aphreet.c3.platform.backup.{BackupLocation, AbstractBackup}
import org.aphreet.c3.platform.common.Disposable._
import org.aphreet.c3.platform.common.Logger

class RemoteBackup(val name: String, val create: Boolean, val config: BackupLocation,
                   val port: Int = -1, val password: String = null) extends AbstractBackup {

  val HOST = config.host
  val USER = config.user
  val REMOTE_FOLDER = config.folder
  val PRIVATE_KEY = config.privateKey

  val connector = new SftpConnector(HOST, USER, PRIVATE_KEY, port)


  {
    zipFilePath = System.getProperty("java.io.tmpdir") + "/" + name
    md5FilePath = zipFilePath + ".md5"

    if (!connector.isConnected) {
      if (password == null) {
        connector.connect()
      } else {
        connector.connect(password)
      }
    }

    if (create) {
      connector.makeDir(REMOTE_FOLDER)
    } else { // open
      if (RemoteBackup.hasValidChecksum(connector, REMOTE_FOLDER, name)) {
        connector.getFile(zipFilePath, REMOTE_FOLDER, name)
      } else {
        throw new IllegalStateException("Remote backup " + name + " doesn't have valid checksum")
      }
    }

    val env = new util.HashMap[String, String]()
    env.put("create", create.toString)
    zipFs = FileSystems.newFileSystem(URI.create("jar:file:" + zipFilePath), env, null)
  }

  override def close() {
    super.close()

    if (!connector.isConnected) {
      connector.connect()
    }

    connector.putFile(zipFilePath, REMOTE_FOLDER)
    connector.putFile(md5FilePath, REMOTE_FOLDER)
    connector.disconnect()

    Files.deleteIfExists(new File(zipFilePath).toPath)
    Files.deleteIfExists(new File(md5FilePath).toPath)
  }
}


object RemoteBackup {

  val log = Logger(getClass)

  def open(name: String, config: BackupLocation): RemoteBackup = {
    new RemoteBackup(name, false, config)
  }

  def create(name: String, config: BackupLocation): RemoteBackup = {
    new RemoteBackup(name, true, config)
  }

  def open(name: String, config: BackupLocation, port: Int, password: String): RemoteBackup = {
    new RemoteBackup(name, false, config, port, password)
  }

  def create(name: String, config: BackupLocation, port: Int, password: String): RemoteBackup = {
    new RemoteBackup(name, true, config, port, password)
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
