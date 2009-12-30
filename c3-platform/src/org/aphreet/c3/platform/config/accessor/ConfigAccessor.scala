package org.aphreet.c3.platform.config.accessor

import java.io.{File, FileWriter} 

trait ConfigAccessor[T] {

  def loadConfig(configDir:File):T
  
  def storeConfig(data:T, configDir:File)

  def writeToFile(text:String, configFile:File) = {
    
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
