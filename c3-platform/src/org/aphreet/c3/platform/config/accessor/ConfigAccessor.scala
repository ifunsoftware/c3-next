package org.aphreet.c3.platform.config.accessor

import java.io.{File, FileWriter}
import org.aphreet.c3.platform.config.PlatformConfigManager


trait ConfigAccessor[T] {
  
  def load:T = loadConfig(getConfigManager.configDir)
  
  def store(data:T) = storeConfig(data, getConfigManager.configDir)
  
  def update(f:Function1[T, T]) = store(f.apply(load))
  
  def loadConfig(configDir:File):T
  
  def storeConfig(data:T, configDir:File)

  def getConfigManager:PlatformConfigManager

  protected def writeToFile(text:String, configFile:File) = {
    
    if(!configFile.exists)
      configFile.createNewFile
    
    
    val fileWriter = new FileWriter(configFile, false)
    try{
      fileWriter write text
      fileWriter.flush
    }finally{
      fileWriter.close
    }
  }
}
