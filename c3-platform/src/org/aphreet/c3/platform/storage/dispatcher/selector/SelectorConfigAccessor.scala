package org.aphreet.c3.platform.storage.dispatcher.selector

import java.io.{StringWriter, File}

import scala.collection.mutable.HashMap
import scala.collection.jcl.Set

import org.aphreet.c3.platform.common.{Path, JSONFormatter}
import org.aphreet.c3.platform.config.accessor.ConfigAccessor

import com.springsource.json.parser.{Node, MapNode, ListNode, AntlrJSONParser, ScalarNode}
import com.springsource.json.writer.JSONWriterImpl;

abstract class SelectorConfigAccessor[T] extends ConfigAccessor[Map[T, (String, Boolean)]]{

   def loadConfig(configDir:File):Map[T, (String, Boolean)] = {
    val file = new File(configDir, filename)
    
    if(file.exists){
      val node = new AntlrJSONParser().parse(file).asInstanceOf[MapNode]
      
      val entries = 
        for(key <- Set.apply(node.getKeys))
          yield (
        	keyFromString(key),
        	(
        	  getArrayValue[String](node, key, 0),
        	  getArrayValue[Boolean](node, key, 1)
        	)
         
          )
      Map[T, (String, Boolean)]() ++ entries
    }else{
      Map[T, (String, Boolean)]()
    }
  }
   
  def keyFromString(string:String):T
  
  def keyToString(key:T):String
  
  def filename:String
  

  private def getArrayValue[T](node:MapNode, key:String, num:Int):T = {
    node.getNode(key).asInstanceOf[ListNode].getNodes.get(num).asInstanceOf[ScalarNode].getValue[T]
  }

  def storeConfig(data:Map[T, (String,Boolean)], configDir:File) = {
    this.synchronized{
    	
      
	    val swriter = new StringWriter()
	    try{
		    val writer = new JSONWriterImpl(swriter)
		    
		    writer.`object`
	
		    for(entry <- data){
		    	writer.key(keyToString(entry._1))
		    	writer.array
		    	writer.value(entry._2._1)
		    	writer.value(entry._2._2)
		    	writer.endArray
		    }
		   
		    writer.endObject
		    
		    swriter.flush
      
		    val result = JSONFormatter.format(swriter.toString)
		    
		    writeToFile(result, new File(configDir, filename))
      
	    }finally{
	    	swriter.close
	    }
    }
  }
}
