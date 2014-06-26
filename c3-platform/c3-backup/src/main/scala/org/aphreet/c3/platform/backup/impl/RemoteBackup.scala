package org.aphreet.c3.platform.backup.impl

import java.io.File
import java.net.URI
import java.nio.file._
import java.util
import org.aphreet.c3.platform.backup.ssh.SftpConnector
import org.aphreet.c3.platform.backup.{RemoteBackupLocation, AbstractBackup}
import org.aphreet.c3.platform.common.{Disposable, Logger}
import scala.collection.mutable.ListBuffer

class RemoteBackup(val name: String, val create: Boolean, val config: RemoteBackupLocation)
  extends AbstractBackup {

  val HOST = config.host
  val USER = config.user
  val REMOTE_FOLDER = config.folder
  val PRIVATE_KEY = config.privateKey

  val connector = new SftpConnector(HOST, USER, PRIVATE_KEY, config.port)


  {
    zipFilePath = System.getProperty("java.io.tmpdir") + "/" + name
    md5FilePath = zipFilePath + ".md5"

    if (!connector.isConnected) {
      if (config.password == null) {
        connector.connect()
      } else {
        connector.connect(config.password)
      }
    }

    if (create) {
      connector.makeDir(REMOTE_FOLDER)
    } else {
      // open
      if (connector.hasValidChecksum(REMOTE_FOLDER, name)) {
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

  val log = Logger(classOf[RemoteBackup])

  def listBackups(target: RemoteBackupLocation): List[String] = {
    val listBuffer = new ListBuffer[String]()

    Disposable.using(new SftpConnector(target.host, target.user, target.privateKey))(connector => {
      connector.connect()

      val allFilesNames = connector.listFiles(target.folder)

      log.info("All file Names in folder:")
      for (name <- allFilesNames) {
        log.info(name)
      }

      allFilesNames
        .filter(fileName => fileName.endsWith(".zip")
        && !allFilesNames.find(str => str.equals(fileName + ".md5")).isEmpty
        && connector.hasValidChecksum(target.folder, fileName))

        .foreach(fileName => listBuffer += fileName)

      log.info("Filtered file names in folder:")
      for (name <- listBuffer) {
        log.info(name)
      }

      listBuffer.toList
    })

  }

}

