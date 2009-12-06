package org.aphreet.c3.platform.config

import java.io.{File, StringWriter, FileWriter}

import java.util.{List => JList}

import scala.collection.jcl.{LinkedList, Conversions, Set, HashMap}

import org.aphreet.c3.platform.storage.{StorageParams, StorageMode, StorageModeParser}

import com.springsource.json.parser.{Node, MapNode, ListNode, AntlrJSONParser, ScalarNode}
import com.springsource.json.writer.JSONWriterImpl;

import org.apache.commons.logging.LogFactory

import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class PlatformConfigManager {
  
  val STORAGE_CONFIG = "c3-storage-config.json"
  
  val PLATFORM_CONFIG = "c3-platform-config.json"

  val log = LogFactory getLog getClass
  
  var configPath:String = "";
  
  @PostConstruct 
  def init = {
    configPath = System.getProperty("c3.home")
    
    if(configPath == null){
      log warn "Config path is not set. Using default path"
      configPath = "C:/var/c3-data"
    }
    
    val configDir = new File(configPath)
    if(!configDir.exists) configDir.mkdirs
    
    log info "Configuration path: " + configPath  
  }
  
  def getStorageParams:List[StorageParams] = {
    val configFile = new File(configPath, STORAGE_CONFIG)
    
    if(configFile.exists){
      val node = new AntlrJSONParser().parse(configFile)
      val storageArray = node.asInstanceOf[MapNode].getNode("storages").asInstanceOf[ListNode].getNodes.toArray
      
      var list:List[StorageParams] = List()
      
      for(st <- storageArray){
    	val storage = st.asInstanceOf[MapNode]
    	 
    	val ids = Conversions.convertList(storage.getNode("ids").asInstanceOf[ListNode].getNodes.asInstanceOf[JList[ScalarNode]])
    	  
        val idArray = for(node <- ids)
        	  yield node.getValue.toString  	                                                                              
       
    	list = list ::: List( 
    	  new StorageParams(
    	    storage.getNode("id").asInstanceOf[ScalarNode].getValue.toString,
    	    List.fromIterator(idArray.elements),
    		storage.getNode("path").asInstanceOf[ScalarNode].getValue.toString.replaceAll("\\+", "\\"),
    		storage.getNode("type").asInstanceOf[ScalarNode].getValue.toString,
    		StorageModeParser.valueOf(storage.getNode("mode").asInstanceOf[ScalarNode].getValue.toString)
         ))
      }
      
      
      list
    }else{
      List()
    }
  }
  
  def setStorageParams(params :List[StorageParams]) = {
    this.synchronized{
    	val file = new File(configPath, STORAGE_CONFIG)
      
	    val swriter = new FileWriter(file)
	    try{
		    val writer = new JSONWriterImpl(swriter)
		    
		    writer.`object`.key("storages").array
		    
		    
		    for(storage <- params){
		      writer.`object`
		        .key("id").value(storage.id)
		        .key("path").value(storage.path)
		        .key("type").value(storage.storageType)
		        .key("mode").value(storage.mode.name)
		        .key("ids").array
		        for(id <- storage.secIds)
		          writer.value(id)
		        writer.endArray
		        
		      writer.endObject
		    }
		    writer.endArray
		    writer.endObject
		    
		    swriter.flush
	    }finally{
	    	swriter.close
	    }
    }
  }
  
  def getPlatformParam:HashMap[String, String] = {
    
    val map = new HashMap[String, String]
    
    val file = new File(configPath, PLATFORM_CONFIG)
    
    if(file.exists){
      val node = new AntlrJSONParser().parse(file).asInstanceOf[MapNode]
      
      for(key <- Set.apply(node.getKeys)){
        val value = node.getNode(key).asInstanceOf[ScalarNode].getValue.toString
        map.put(key, value)
      }
      
    }
    map
  }
  
  def setPlatformParam(map:HashMap[String, String]) = {
    this.synchronized{
      val file = new File(configPath, PLATFORM_CONFIG)
      
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
