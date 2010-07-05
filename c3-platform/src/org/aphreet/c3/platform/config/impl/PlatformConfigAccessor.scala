/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.aphreet.c3.platform.config.impl

import org.aphreet.c3.platform.common.{JSONFormatter}

import java.io.{File, FileWriter, StringWriter}


import scala.collection.jcl.Set
import scala.collection.immutable.Map

import com.springsource.json.parser.{MapNode, AntlrJSONParser, ScalarNode}
import com.springsource.json.writer.JSONWriterImpl
import org.aphreet.c3.platform.config._

import org.springframework.stereotype.Component

@Component
class PlatformConfigAccessor extends ConfigAccessor[Map[String,String]]{

  val PLATFORM_CONFIG = "c3-platform-config.json"

  var configManager:PlatformConfigManager = null


  var configDirectory:File = _

  def configDir:File = configDirectory

  def loadConfig(configDir:File):Map[String, String] = {
    var map = Map[String, String]()
    
    val file = new File(configDir, PLATFORM_CONFIG)
    
    if(file.exists){
      val node = new AntlrJSONParser().parse(file).asInstanceOf[MapNode]

      for(key <- Set.apply(node.getKeys)){
        val value = node.getNode(key).asInstanceOf[ScalarNode].getValue.toString
        map = map + ((key, value))
      }
    }
    map
  }
  
  def storeConfig(map:Map[String, String], configDir:File) = {
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