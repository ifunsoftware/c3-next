package org.aphreet.c3.platform.storage.dispatcher.selector.mime

import java.io.File
import java.io.StringWriter

import scala.collection.Map
import scala.collection.mutable.HashMap
import scala.collection.jcl.Set

import org.aphreet.c3.platform.common.{Path, JSONFormatter}
import org.aphreet.c3.platform.config.accessor.ConfigAccessor

import com.springsource.json.parser.{Node, MapNode, ListNode, AntlrJSONParser, ScalarNode}
import com.springsource.json.writer.JSONWriterImpl;

import org.springframework.stereotype.Component

@Component
class MimeTypeConfigAccessor extends ConfigAccessor[List[MimeConfigEntry]]{

  private val MIME_CONFIG = "mime-types.json"
  
  def loadConfig(configDir:File):List[MimeConfigEntry] = {
    val file = new File(configDir, MIME_CONFIG)
    
    if(file.exists){
      val node = new AntlrJSONParser().parse(file).asInstanceOf[MapNode]
      
      val entries = 
        for(key <- Set.apply(node.getKeys))
          yield MimeConfigEntry(
        	key,
        	getArrayValue[String](node, key, 0),
        	getArrayValue[Boolean](node, key, 1)
          )
      List.fromIterator(entries.elements)
    }else{
      List()
    }
  }

  private def getArrayValue[T](node:MapNode, key:String, num:Int):T = {
    node.getNode(key).asInstanceOf[ListNode].getNodes.get(num).asInstanceOf[ScalarNode].getValue[T]
  }

  def storeConfig(data:List[MimeConfigEntry], configDir:File) = {
    this.synchronized{
    	
      
	    val swriter = new StringWriter()
	    try{
		    val writer = new JSONWriterImpl(swriter)
		    
		    writer.`object`
	
		    for(entry <- data){
		    	writer.key(entry.mimeType)
		    	writer.array
		    	writer.value(entry.storage)
		    	writer.value(entry.versioned)
		    	writer.endArray
		    }
		   
		    writer.endObject
		    
		    swriter.flush
      
		    val result = JSONFormatter.format(swriter.toString)
		    
		    writeToFile(result, new File(configDir, MIME_CONFIG))
      
	    }finally{
	    	swriter.close
	    }
    }
  }
  
}
