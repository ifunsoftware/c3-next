package org.aphreet.c3.platform.search.impl

import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigAccessor}
import org.springframework.stereotype.Component
import java.io.File
import org.springframework.beans.factory.annotation.Autowired

@Component
class SearchConfigurationAccessor extends ConfigAccessor[Map[String, (Int, Int)]]{

  @Autowired
  var configManager:PlatformConfigManager = _

  protected def configFileName = "c3-search-config.json"

  protected def configDir = configManager.configDir

  protected def defaultConfig = Map()

  protected def loadConfig(configFile: File):Map[String, (Int, Int)] {

  }

  protected def storeConfig(data: Map[String, (Int, Int)], configFile: File) {

  }
}
