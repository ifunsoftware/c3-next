package org.aphreet.c3.platform.config.accessor

import java.io.{File, StringWriter}
import java.util.{List => JList}

import scala.collection.jcl.{LinkedList, Conversions, Set, HashMap}

import org.aphreet.c3.platform.common.{Path, JSONFormatter}
import org.aphreet.c3.platform.storage.{StorageParams, StorageMode, StorageModeParser}

import com.springsource.json.parser.{Node, MapNode, ListNode, AntlrJSONParser, ScalarNode}
import com.springsource.json.writer.JSONWriterImpl;

class StorageConfigAccessor extends ConfigAccessor[List[StorageParams]]{

  val STORAGE_CONFIG = "c3-storage-config.json"
  
  def loadConfig(configDir:File):List[StorageParams] = {
    val configFile = new File(configDir, STORAGE_CONFIG)
    
    if(configFile.exists){
      val node = new AntlrJSONParser().parse(configFile)
      val storageArray = node.asInstanceOf[MapNode].getNode("storages").asInstanceOf[ListNode].getNodes.toArray
      
      var list:List[StorageParams] = List()
      
      for(st <- storageArray){
    	val storage = st.asInstanceOf[MapNode]
    	 
    	val ids = Conversions.convertList(storage.getNode("ids").asInstanceOf[ListNode].getNodes.asInstanceOf[JList[ScalarNode]])
    	  
        val idArray = for(node <- ids)
        	  yield node.getValue.toString  	                                                                              
        
        val storageModeName = storage.getNode("mode").asInstanceOf[ScalarNode].getValue.toString
        
        var storageModeMessage = ""
        
        val storageModeMessageNode = storage.getNode("modemsg")
        
        
        if(storageModeMessageNode != null){
          storageModeMessage = storageModeMessageNode.asInstanceOf[ScalarNode].getValue.toString
        }
        
        val storageMode = StorageModeParser.valueOf(storageModeName, storageModeMessage)
        
        
    	list = list ::: List( 
    	  new StorageParams(
    	    storage.getNode("id").asInstanceOf[ScalarNode].getValue.toString,
    	    List.fromIterator(idArray.elements),
    		new Path(storage.getNode("path").asInstanceOf[ScalarNode].getValue.toString),
    		storage.getNode("type").asInstanceOf[ScalarNode].getValue.toString,
    		storageMode
         ))
      }
      
      
      list
    }else{
      List()
    }
  }
  
  def storeConfig(params:List[StorageParams], configDir:File) = {
    this.synchronized{
    	
      
	    val swriter = new StringWriter()
	    try{
		    val writer = new JSONWriterImpl(swriter)
		    
		    writer.`object`.key("storages").array
		    
		    
		    for(storage <- params){
		      writer.`object`
		        .key("id").value(storage.id)
		        .key("path").value(storage.path)
		        .key("type").value(storage.storageType)
		        .key("mode").value(storage.mode.name)
		        .key("modemsg").value(storage.mode.message)
		        .key("ids").array
		        for(id <- storage.secIds)
		          writer.value(id)
		        writer.endArray
		        
		      writer.endObject
		    }
		    writer.endArray
		    writer.endObject
		    
		    swriter.flush
      
		    val result = JSONFormatter.format(swriter.toString)
		    
		    writeToFile(result, new File(configDir, STORAGE_CONFIG))
      
	    }finally{
	    	swriter.close
	    }
    }
  }
}
