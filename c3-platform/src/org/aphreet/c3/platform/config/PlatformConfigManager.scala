package org.aphreet.c3.platform.config

import java.io.File

import scala.collection.Map
import scala.collection.jcl.HashMap

import accessor.ConfigAccessor

import org.aphreet.c3.platform.common.{Path, Constants}
import org.aphreet.c3.platform.exception.ConfigurationException
import org.aphreet.c3.platform.storage.StorageParams

import accessor._

import org.apache.commons.logging.LogFactory

import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class PlatformConfigManager {
  
  val log = LogFactory getLog getClass
  
  var configPath:String = "";
  var configDir:File = null;
  
  @PostConstruct 
  def init = {
    configPath = System.getProperty("c3.home")
    
    if(configPath == null){
      log info "Config path is not set. Using user home path"
      configPath = System.getProperty("user.home") + File.separator + ".c3"
    }
    
    if(configPath == null){
      throw new ConfigurationException("Can't find path to store config")
    }else{
      log info "Using " + configPath + " to store C3 configuration"
    }
    
    val path = new Path(configPath)
    
    configPath = path.toString
    configDir = path.file
    if(!configDir.exists) configDir.mkdirs
  }
  
}
