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
package org.aphreet.c3.platform.storage.dispatcher.selector

import java.io.{StringWriter, File}

import collection.JavaConversions

import org.aphreet.c3.platform.common.JSONFormatter
import org.aphreet.c3.platform.config.ConfigAccessor
import org.aphreet.c3.platform.config.PlatformConfigManager

import com.springsource.json.parser.{MapNode, ListNode, AntlrJSONParser, ScalarNode}
import com.springsource.json.writer.JSONWriterImpl

import org.springframework.beans.factory.annotation.Autowired;

abstract class SelectorConfigAccessor[T] extends ConfigAccessor[Map[T, (String, Boolean)]] {
  var configManager: PlatformConfigManager = null

  @Autowired
  def setConfigManager(manager: PlatformConfigManager) {configManager = manager}

  def configDir: File = configManager.configDir

  def defaultConfig:Map[T, (String, Boolean)] = Map()

  def loadConfig(configFile: File): Map[T, (String, Boolean)] = {

    val node = new AntlrJSONParser().parse(configFile).asInstanceOf[MapNode]

    val entries =
      for (key <- JavaConversions.asSet(node.getKeys))
        yield (
            keyFromString(key),
            (
                    getArrayValue[String](node, key, 0),
                    getArrayValue[Boolean](node, key, 1)
                    )

            )
    Map[T, (String, Boolean)]() ++ entries

  }

  def keyFromString(string: String): T

  def keyToString(key: T): String


  private def getArrayValue[T](node: MapNode, key: String, num: Int): T = {
    node.getNode(key).asInstanceOf[ListNode].getNodes.get(num).asInstanceOf[ScalarNode].getValue[T]
  }

  def storeConfig(data: Map[T, (String, Boolean)], configFile: File) {
    this.synchronized {


      val swriter = new StringWriter()
      try {
        val writer = new JSONWriterImpl(swriter)

        writer.`object`

        for (entry <- data) {
          writer.key(keyToString(entry._1))
          writer.array
          writer.value(entry._2._1)
          writer.value(entry._2._2)
          writer.endArray
        }

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
