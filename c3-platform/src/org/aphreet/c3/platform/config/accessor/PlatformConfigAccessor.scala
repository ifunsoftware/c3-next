package org.aphreet.c3.platform.config.accessor

import java.io.{File, FileWriter}

import scala.collection.jcl.{LinkedList, Conversions, Set, HashMap}

import com.springsource.json.parser.{Node, MapNode, ListNode, AntlrJSONParser, ScalarNode}
import com.springsource.json.writer.JSONWriterImpl;


class PlatformConfigAccessor extends ConfigAccessor[HashMap[String,String]]{

  val PLATFORM_CONFIG = "c3-platform-config.json"
  
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
      val file = new File(configDir, PLATFORM_CONFIG)
      
      val swriter = new FileWriter(file)
      
      try{
        val writer = new JSONWriterImpl(swriter)
        
        writer.`object`
        
        map.foreach((e:(String, String)) => writer.key(e._1).value(e._2))
        
        writer.endObject
        
        swriter.flush
        
      }finally{
        swriter.close
      }
    }
  }
}
