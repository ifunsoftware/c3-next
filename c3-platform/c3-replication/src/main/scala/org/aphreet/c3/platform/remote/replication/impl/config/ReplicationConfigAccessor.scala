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

import org.aphreet.c3.platform.config.{ConfigPersister, PlatformConfigManager, ConfigAccessor}
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import com.springsource.json.writer.JSONWriter
import com.springsource.json.parser._
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope
import collection.JavaConversions._
import org.aphreet.c3.platform.remote.replication.ReplicationHost

abstract class ReplicationConfigAccessor extends ConfigAccessor[Map[String, ReplicationHost]]{

  override def defaultConfig:Map[String, ReplicationHost] = Map()

  def readConfig(node: Node):Map[String, ReplicationHost] = {

    var map = Map[String, ReplicationHost]()

    val hostsListNode = node.getNode("hosts")

    for (hostNode <- hostsListNode.getNodes) {
      val host = getValue(hostNode, "host")
      val key = getValue(hostNode, "key")
      val id = getValue(hostNode, "id")
      val http = getValue(hostNode, "http_port").toInt
      val https = getValue(hostNode, "https_port").toInt
      val replicationPort = getValue(hostNode, "replication_port").toInt
      val sharedKey = getValue(hostNode, "shared_key")

      map = map + ((id, new ReplicationHost(id, host, key, http, https, replicationPort, sharedKey)))
    }
    map
  }

  def writeConfig(map:Map[String, ReplicationHost], writer: JSONWriter) {
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
  }

  private def getValue(node: MapNode, key: String): String = {
    node.getNode(key).getValue[String]
  }

}

class ReplicationSourcesConfigAccessor(val persister: ConfigPersister) extends ReplicationConfigAccessor{

  def name = "c3-replication-sources"

}

class ReplicationTargetsConfigAccessor(val persister: ConfigPersister) extends ReplicationConfigAccessor{

  def name = "c3-replication-targets"

}