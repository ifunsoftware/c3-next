package org.aphreet.c3.platform.config.accessor

import java.io.File

trait ConfigAccessor[T] {

  def loadConfig(configDir:File):T
  
  def storeConfig(data:T, configDir:File)

}
