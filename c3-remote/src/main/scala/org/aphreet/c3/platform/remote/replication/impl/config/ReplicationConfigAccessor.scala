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

package org.aphreet.c3.platform.remote.replication.impl.config

import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigAccessor}
import org.springframework.beans.factory.annotation.Autowired
import java.io.{FileWriter, StringWriter, File}
import org.aphreet.c3.platform.common.JSONFormatter
import com.springsource.json.writer.JSONWriterImpl
import com.springsource.json.parser.{ScalarNode, ListNode, MapNode, AntlrJSONParser}
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope
import collection.JavaConversions._
import org.aphreet.c3.platform.remote.api.management.ReplicationHost


abstract class ReplicationConfigAccessor extends ConfigAccessor[Map[String, ReplicationHost]]{

  var configManager: PlatformConfigManager = _

  def configDir: File = configManager.configDir

  @Autowired
  def setConfigManager(manager: PlatformConfigManager) = {configManager = manager}

  override def defaultConfig:Map[String, ReplicationHost] = {
    Map()
  }

  def loadConfig(configFile:File):Map[String, ReplicationHost] = {

    var map = Map[String, ReplicationHost]()


    val node = new AntlrJSONParser().parse(configFile).asInstanceOf[MapNode]

    val hostsListNode = node.getNode("hosts").asInstanceOf[ListNode]

    for (hostNode <- asBuffer(hostsListNode.getNodes)) {
      val host = getValue(hostNode.asInstanceOf[MapNode], "host")
      val key = getValue(hostNode.asInstanceOf[MapNode], "key")
      val id = getValue(hostNode.asInstanceOf[MapNode], "id")
      val http = getValue(hostNode.asInstanceOf[MapNode], "http_port").toInt
      val https = getValue(hostNode.asInstanceOf[MapNode], "https_port").toInt
      val replicationPort = getValue(hostNode.asInstanceOf[MapNode], "replication_port").toInt
      val sharedKey = getValue(hostNode.asInstanceOf[MapNode], "shared_key")

      map = map + ((id, new ReplicationHost(id, host, key, http, https, replicationPort, sharedKey)))
    }
    map
  }

  def storeConfig(map:Map[String, ReplicationHost], configFile: File) = {
    this.synchronized {
      val swriter = new StringWriter()

      var fileWriter: FileWriter = null

      try {
        val writer = new JSONWriterImpl(swriter)

        writer.`object`

        writer.key("hosts")

        writer.array

        for ((id, host) <- map) {
          writer.`object`

          writer.key("id")
          writer.value(host.systemId)

          writer.key("host")
          writer.value(host.hostname)

          writer.key("key")
          writer.value(host.key)

          writer.key("http_port")
          writer.value(host.httpPort)

          writer.key("https_port")
          writer.value(host.httpsPort)

          writer.key("replication_port")
          writer.value(host.replicationPort)

          writer.key("shared_key")
          writer.value(host.encryptionKey)

          writer.endObject
        }
        writer.endArray
        writer.endObject


        swriter.flush

        val result = JSONFormatter.format(swriter.toString)

        writeToFile(result, configFile)

      } finally {
        swriter.close
      }
    }
  }

  private def getValue(node: MapNode, key: String): String = {
    node.getNode(key).asInstanceOf[ScalarNode].getValue[String]
  }

}


@Component
@Scope("singleton")
class ReplicationSourcesConfigAccessor extends ReplicationConfigAccessor{

  def configFileName: String = "c3-replication-sources.json"

}

@Component
@Scope("singleton")
class ReplicationTargetsConfigAccessor extends ReplicationConfigAccessor{

  def configFileName: String = "c3-replication-targets.json"

}