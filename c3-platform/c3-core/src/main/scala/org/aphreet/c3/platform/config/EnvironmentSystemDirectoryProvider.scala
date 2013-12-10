package org.aphreet.c3.platform.config

import java.io.File
import org.aphreet.c3.platform.exception.ConfigurationException
import org.aphreet.c3.platform.common.{Logger, Path}

/**
 * Author: Mikhail Malygin
 * Date:   12/9/13
 * Time:   4:11 PM
 */
trait EnvironmentSystemDirectoryProvider extends SystemDirectoryProvider{

  def configurationDirectory = EnvironmentSystemDirectoryProvider.configurationDirectory

  def dataDirectory = EnvironmentSystemDirectoryProvider.dataDirectory

}

object EnvironmentSystemDirectoryProvider{

  val log = Logger(getClass)

  val dataDirectory = {
    val dataPath = new Path(System.getProperty("c3.data") match {
      case value: String => value
      case null => System.getProperty("user.home") + File.separator + ".c3data"
    })


    if(!dataPath.file.exists()){
      dataPath.file.mkdirs()
    }

    dataPath.file
  }

  val configurationDirectory = {
    var configPath = System.getProperty("c3.home")

    if (configPath == null) {
      log info "Config path is not set. Using user home path"
      configPath = System.getProperty("user.home") + File.separator + ".c3"
    }

    if (configPath == null) {
      throw new ConfigurationException("Can't find path to store config")
    } else {
      log info "Using " + configPath + " to store C3 configuration"
    }

    val path = new Path(configPath)

    if (!path.file.exists) path.file.mkdirs
    path.file
  }

}
