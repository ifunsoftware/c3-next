package org.aphreet.c3.platform.config.accessor

import org.aphreet.c3.platform.common.{JSONFormatter, Constants}

import java.io.{File, FileWriter, StringWriter}

import scala.collection.jcl.{LinkedList, Conversions, Set, HashMap}

import com.springsource.json.parser.{Node, MapNode, ListNode, AntlrJSONParser, ScalarNode}
import com.springsource.json.writer.JSONWriterImpl
import org.aphreet.c3.platform.config.PlatformConfigManager
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class PlatformConfigAccessor extends ConfigAccessor[HashMap[String,String]]{

  val PLATFORM_CONFIG = "c3-platform-config.json"

  var configManager:PlatformConfigManager = null

  def getConfigManager:PlatformConfigManager = configManager

  @Autowired
  def setConfigManager(manager:PlatformConfigManager) = {configManager = manager}

  @PostConstruct
  def init = {
    val props = load
    props.put(Constants.C3_PLATFORM_HOME, configManager.configPath)
    store(props)
  }
  
  def loadConfig(configDir:File):HashMap[String, String] = {
    val map = new HashMap[String, String]
    
    val file = new File(configDir, PLATFORM_CONFIG)
    
    if(file.exists){
      val node = new AntlrJSONParser().parse(file).asInstanceOf[MapNode]
      
      for(key <- Set.apply(node.getKeys)){
        val value = node.getNode(key).asInstanceOf[ScalarNode].getValue.toString
        map.put(key, value)
      }
    }
    map
  }
  
  def storeConfig(map:HashMap[String, String], configDir:File) = {
    this.synchronized{
      val swriter = new StringWriter()
      
      var fileWriter:FileWriter = null
      
      try{
        val writer = new JSONWriterImpl(swriter)
        
        writer.`object`
        
        map.foreach((e:(String, String)) => writer.key(e._1).value(e._2))
        
        writer.endObject
        
        swriter.flush
        
        val result = JSONFormatter.format(swriter.toString)
        
        val file = new File(configDir, PLATFORM_CONFIG)
        
        writeToFile(result, file)
        
      }finally{
        swriter.close
      }
    }
  }
  
}
