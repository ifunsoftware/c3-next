package org.aphreet.c3.platform.config.impl

import org.aphreet.c3.platform.config.{SystemDirectoryProvider, ConfigPersister}
import java.io.File
import java.nio.file.{StandardOpenOption, Files}

/**
 * Author: Mikhail Malygin
 * Date:   12/16/13
 * Time:   3:49 PM
 */
class FileConfigPersister(val directoryProvider: SystemDirectoryProvider) extends ConfigPersister{

  def writeConfig(name: String, config: String) {
    Files.write(configFile(name).toPath, config.getBytes("UTF-8"),
      StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
  }

  def readConfig(name: String): String =
    new String(Files.readAllBytes(configFile(name).toPath), "UTF-8")

  def configExists(name: String): Boolean =
    configFile(name).exists()

  private def configFile(name: String): File =
    new File(directoryProvider.configurationDirectory, name + ".json")
}
