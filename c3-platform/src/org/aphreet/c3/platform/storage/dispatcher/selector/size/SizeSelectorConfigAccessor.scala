package org.aphreet.c3.platform.storage.dispatcher.selector.size

import java.io.File
import java.io.StringWriter

import org.aphreet.c3.platform.common.JSONFormatter
import org.aphreet.c3.platform.config.accessor.ConfigAccessor

import scala.collection.jcl.Set

import com.springsource.json.parser.{Node, MapNode, ListNode, AntlrJSONParser, ScalarNode}
import com.springsource.json.writer.JSONWriterImpl

import org.springframework.stereotype.Component

@Component
class SizeSelectorConfigAccessor extends ConfigAccessor[Map[Long,String]]{

  private val SIZE_CONFIG = "c3-size-types.json"
  
  def loadConfig(configDir:File):Map[Long,String] = {
     val file = new File(configDir, SIZE_CONFIG)
    
    if(file.exists){
      val node = new AntlrJSONParser().parse(file).asInstanceOf[MapNode]
      
      val keys:Set[String] = Set.apply(node.getKeys)
      
      val entries = 
        for(key <- keys)
          yield (key.toLong, node.getNode(key).asInstanceOf[ScalarNode].getValue[String])
      
      
      Map[Long, String]() ++ entries
    }else{
      Map[Long, String]()
    }
    
    
  }
  
  
  def storeConfig(data:Map[Long,String], configDir:File) = {
    this.synchronized{
    	
      
	    val swriter = new StringWriter()
	    try{
		    val writer = new JSONWriterImpl(swriter)
		    
		    writer.`object`
	
		    for(entry <- data){
		    	writer.key(String.valueOf(entry._1))
		    	writer.value(entry._2)
		    }
		   
		    writer.endObject
		    
		    swriter.flush
      
		    val result = JSONFormatter.format(swriter.toString)
		    
		    writeToFile(result, new File(configDir, SIZE_CONFIG))
      
	    }finally{
	    	swriter.close
	    }
    }
  }
}
