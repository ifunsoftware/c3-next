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

import java.io.File
import java.util.{List => JList}

import org.aphreet.c3.platform.common.Path
import com.springsource.json.parser._
import com.springsource.json.writer.JSONWriter

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.config.PlatformConfigManager
import collection.JavaConversions._
import org.aphreet.c3.platform.storage.{StorageIndex, StorageConfigAccessor, StorageParams, StorageModeParser}
import collection.mutable.HashMap


@Component
class StorageConfigAccessorImpl extends StorageConfigAccessor {

  @Autowired
  var configManager: PlatformConfigManager = _

  def configFileName: String = "c3-storage-config.json"

  def configDir: File = configManager.configDir

  def defaultConfig:List[StorageParams] = List()

  def readConfig(node:Node): List[StorageParams] = {

    val storageArray = node.getNode("storages").getNodes

    var list: List[StorageParams] = List()

    for (storage <- storageArray) {
      val storageModeName = storage.getNode("mode").getValue[String]

      var storageModeMessage = ""

      val storageModeMessageNode = storage.getNode("modemsg")


      if (storageModeMessageNode != null) {
        storageModeMessage = storageModeMessageNode.getValue[String]
      }

      val storageMode = StorageModeParser.valueOf(storageModeName, storageModeMessage)


      var indexes:List[StorageIndex] = List()

      val indexesNode = storage.getNode("indexes")

      if(indexesNode != null){

        val indexMaps = asScalaBuffer(
          indexesNode.getNodes.asInstanceOf[JList[MapNode]])

        for (indexMap <- indexMaps){
          val indexName = indexMap.getNode("name").getValue[String]
          val mulIndex =  indexMap.getNode("multi").getValue[Boolean]
          val system = indexMap.getNode("system").getValue[Boolean]
          val created:Long = indexMap.getNode("created").getValue[String].toLong

          val fieldList =
            indexMap.getNode("fields").getNodes.map(_.getValue[String]).toList

          indexes = indexes ::: List(new StorageIndex(indexName, fieldList, mulIndex, system, created))
        }
      }

      val repParameters = new HashMap[String, String]

      val repParamsNode = storage.getNode("params")

      if (repParamsNode != null) {
        for (i <- 0 to 2) {
          if (repParamsNode.getNode("nodeName-"  + i) != null)
            repParameters.put("nodeName-" + i,
              repParamsNode.getNode("nodeName-"  + i).getValue[String])

          if (repParamsNode.getNode("nodePort-"  + i) != null)
            repParameters.put("nodePort-" + i,
              repParamsNode.getNode("nodePort-"  + i).getValue[String])

          if (repParamsNode.getNode("nodeDir-"  + i) != null)
            repParameters.put("nodeDir-" + i,
              repParamsNode.getNode("nodeDir-"  + i).getValue[String])
        }
        if (repParamsNode.getNode("nodeCounter") != null)
          repParameters.put("nodeCounter",
            repParamsNode.getNode("nodeCounter").getValue[String])
      }

      list = list ::: List(
        new StorageParams(
          storage.getNode("id").getValue[String],
          new Path(storage.getNode("path").getValue[String]),
          storage.getNode("type").getValue[String],
          storageMode,
          indexes,
          repParameters
        ))
    }


    list
  }

  def writeConfig(params: List[StorageParams], jsonWriter:JSONWriter) {
    jsonWriter.`object`.key("storages").array

    for (storage <- params) {
      jsonWriter.`object`
        .key("id").value(storage.id)
        .key("path").value(storage.path)
        .key("type").value(storage.storageType)
        .key("mode").value(storage.mode.name)
        .key("modemsg").value(storage.mode.message)

      jsonWriter.key("indexes").array //indexes start
      for(index <- storage.indexes){
        jsonWriter.`object`
          .key("name").value(index.name)
          .key("multi").value(index.multi)
          .key("system").value(index.system)
          .key("created").value(index.created)
          .key("fields").array
        for(field <- index.fields)
          jsonWriter.value(field)
        jsonWriter.endArray
        jsonWriter.endObject
      }

      jsonWriter.endArray //indexes end

      /* new */
      jsonWriter.key("params").`object`
      for((paramKey, paramValue) <- storage.params) {
        jsonWriter.key(paramKey).value(paramValue)
      }
      jsonWriter.endObject
      /* end of new */

      jsonWriter.endObject
    }
    jsonWriter.endArray
    jsonWriter.endObject
  }
}