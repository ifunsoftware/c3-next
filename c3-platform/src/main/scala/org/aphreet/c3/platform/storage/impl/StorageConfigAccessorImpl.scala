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
package org.aphreet.c3.platform.storage.impl

import java.io.{File, StringWriter}
import java.util.{List => JList}

import org.aphreet.c3.platform.common.{Path, JSONFormatter}
import com.springsource.json.parser._
import com.springsource.json.writer.JSONWriterImpl

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.config.PlatformConfigManager
import collection.JavaConversions
import org.aphreet.c3.platform.storage.{StorageIndex, StorageConfigAccessor, StorageParams, StorageModeParser}
import collection.mutable.{HashMap, Buffer}


@Component
class StorageConfigAccessorImpl extends StorageConfigAccessor {
  var configManager: PlatformConfigManager = null

  @Autowired
  def setConfigManager(manager: PlatformConfigManager) {configManager = manager}

  def configFileName: String = "c3-storage-config.json"

  def configDir: File = configManager.configDir

  def defaultConfig:List[StorageParams] = List()

  def loadConfig(configFile: File): List[StorageParams] = {

    val node = new AntlrJSONParser().parse(configFile)
    val storageArray = node.asInstanceOf[MapNode].getNode("storages").asInstanceOf[ListNode].getNodes.toArray

    var list: List[StorageParams] = List()

    for (st <- storageArray) {
      val storage = st.asInstanceOf[MapNode]

      val ids = JavaConversions.asBuffer(storage.getNode("ids").asInstanceOf[ListNode].getNodes.asInstanceOf[JList[ScalarNode]])

      val idArray = for (node <- ids)
        yield node.getValue.toString

      val storageModeName = storage.getNode("mode").asInstanceOf[ScalarNode].getValue.toString

      var storageModeMessage = ""

      val storageModeMessageNode = storage.getNode("modemsg")


      if (storageModeMessageNode != null) {
        storageModeMessage = storageModeMessageNode.asInstanceOf[ScalarNode].getValue.toString
      }

      val storageMode = StorageModeParser.valueOf(storageModeName, storageModeMessage)



      var indexes:List[StorageIndex] = List()

      val indexesNode = storage.getNode("indexes")

      if(indexesNode != null){

        val indexMaps = JavaConversions.asBuffer(
          indexesNode.asInstanceOf[ListNode].getNodes.asInstanceOf[JList[MapNode]])

        val result = for (indexMap <- indexMaps){
          val indexName = indexMap.getNode("name").asInstanceOf[ScalarNode].getValue[String]
          val mulIndex =  indexMap.getNode("multi").asInstanceOf[ScalarNode].getValue[Boolean]
          val system = indexMap.getNode("system").asInstanceOf[ScalarNode].getValue[Boolean]
          val created:Long = indexMap.getNode("created").asInstanceOf[ScalarNode].getValue[String].toLong

          val fields = JavaConversions.asBuffer(
            indexMap.getNode("fields").asInstanceOf[ListNode].getNodes.asInstanceOf[JList[ScalarNode]])

          val fieldList = fields.map(_.getValue[String]).toList

          indexes = indexes ::: List(new StorageIndex(indexName, fieldList, mulIndex, system, created))
        }
        

      }

      val repParameters = new HashMap[String, String]

      val repParamsNode = storage.getNode("params")

      if (repParamsNode != null) {
        val repParamsMap = repParamsNode.asInstanceOf[MapNode]

        for (i <- 0 to 2) {
          if (repParamsMap.getNode("nodeName-"  + i) != null)
            repParameters.put("nodeName-" + i,
                            repParamsMap.getNode("nodeName-"  + i).asInstanceOf[ScalarNode].getValue.toString)

          if (repParamsMap.getNode("nodePort-"  + i) != null)
            repParameters.put("nodePort-" + i,
                            repParamsMap.getNode("nodePort-"  + i).asInstanceOf[ScalarNode].getValue.toString)

          if (repParamsMap.getNode("nodeDir-"  + i) != null)
            repParameters.put("nodeDir-" + i,
                            repParamsMap.getNode("nodeDir-"  + i).asInstanceOf[ScalarNode].getValue.toString)
        }
        if (repParamsMap.getNode("nodeCounter") != null)
          repParameters.put("nodeCounter",
                            repParamsMap.getNode("nodeCounter").asInstanceOf[ScalarNode].getValue.toString)
      }

      list = list ::: List(
        new StorageParams(
          storage.getNode("id").asInstanceOf[ScalarNode].getValue.toString,
          idArray.toList,
          new Path(storage.getNode("path").asInstanceOf[ScalarNode].getValue.toString),
          storage.getNode("type").asInstanceOf[ScalarNode].getValue.toString,
          storageMode,
          indexes,
          repParameters
          ))
    }


    list
  }

  def storeConfig(params: List[StorageParams], configFile: File) {
    this.synchronized {

      val swriter = new StringWriter()
      try {
        val writer = new JSONWriterImpl(swriter)

        writer.`object`.key("storages").array


        for (storage <- params) {
          writer.`object`
                  .key("id").value(storage.id)
                  .key("path").value(storage.path)
                  .key("type").value(storage.storageType)
                  .key("mode").value(storage.mode.name)
                  .key("modemsg").value(storage.mode.message)
                  .key("ids").array
                    for (id <- storage.secIds)
                       writer.value(id)
                  writer.endArray

          writer.key("indexes").array //indexes start
            for(index <- storage.indexes){
              writer.`object`
                .key("name").value(index.name)
                .key("multi").value(index.multi)
                .key("system").value(index.system)
                .key("created").value(index.created)
                .key("fields").array
                   for(field <- index.fields)
                     writer.value(field)
                writer.endArray
              writer.endObject
            }

          writer.endArray //indexes end

          /* new */
          writer.key("params").`object`
            for((paramKey, paramValue) <- storage.params) {
              writer.key(paramKey).value(paramValue)
            }
          writer.endObject
          /* end of new */

          writer.endObject
        }
        writer.endArray
        writer.endObject

        swriter.flush()

        val result = JSONFormatter.format(swriter.toString)

        writeToFile(result, configFile)

      } finally {
        swriter.close()
      }
    }
  }
}