package org.aphreet.c3.platform.config.accessor

import java.io.{File, FileWriter} 

import org.springframework.beans.factory.annotation.Autowired

abstract class ConfigAccessor[T] {

  var configManager:PlatformConfigManager = null
  
  @Autowired
  def setConfigManager(manager:PlatformConfigManager) = {configManager = manager}
  
  def load:T = loadConfig(configManager.configDir)
  
  def store(data:T) = storeConfig(data, configManager.configDir)
  
  def update(f:Function1[T, T]) = store(f.apply(load))
  
  def loadConfig(configDir:File):T
  
  def storeConfig(data:T, configDir:File)

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
