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
import org.aphreet.c3.platform.storage.{StorageConfigAccessor, StorageParams, StorageModeParser}
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.config.PlatformConfigManager


@Component
class StorageConfigAccessorImpl extends StorageConfigAccessor {
  val STORAGE_CONFIG = "c3-storage-config.json"

  var configManager: PlatformConfigManager = null

  @Autowired
  def setConfigManager(manager: PlatformConfigManager) = {configManager = manager}

  def configDir: File = configManager.configDir

  def loadConfig(configDir: File): List[StorageParams] = {
    val configFile = new File(configDir, STORAGE_CONFIG)

    if (configFile.exists) {
      val node = new AntlrJSONParser().parse(configFile)
      val storageArray = node.asInstanceOf[MapNode].getNode("storages").asInstanceOf[ListNode].getNodes.toArray

      var list: List[StorageParams] = List()

      for (st <- storageArray) {
        val storage = st.asInstanceOf[MapNode]

        val ids = collection.jcl.Conversions.convertList(storage.getNode("ids").asInstanceOf[ListNode].getNodes.asInstanceOf[JList[ScalarNode]])

        val idArray = for (node <- ids)
                          yield node.getValue.toString

        val storageModeName = storage.getNode("mode").asInstanceOf[ScalarNode].getValue.toString

        var storageModeMessage = ""

        val storageModeMessageNode = storage.getNode("modemsg")


        if (storageModeMessageNode != null) {
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
    } else {
      List()
    }
  }

  def storeConfig(params: List[StorageParams], configDir: File) = {
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

          writer.endObject
        }
        writer.endArray
        writer.endObject

        swriter.flush

        val result = JSONFormatter.format(swriter.toString)

        writeToFile(result, new File(configDir, STORAGE_CONFIG))

      } finally {
        swriter.close
      }
    }
  }
}