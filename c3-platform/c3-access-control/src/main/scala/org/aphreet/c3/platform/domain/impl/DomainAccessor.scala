/**
 * Copyright (c) 2011, Mikhail Malygin
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

package org.aphreet.c3.platform.domain.impl

import com.springsource.json.parser._
import com.springsource.json.writer.JSONWriter
import java.util.UUID
import org.aphreet.c3.platform.config.{ConfigPersister, SystemDirectoryProvider, ConfigAccessor}
import org.aphreet.c3.platform.domain.{FullMode, DomainMode, Domain}
import scala.collection.JavaConversions._


class DomainAccessor(val persister: ConfigPersister) extends ConfigAccessor[DomainConfig]{

  def name: String = "c3-domain-config"

  def defaultConfig:DomainConfig = {
    val domainId = UUID.randomUUID.toString

    DomainConfig(List(Domain(domainId, "anonymous", "", FullMode, deleted = false)), domainId)
  }

  def readConfig(node: Node): DomainConfig = {

    val domains = for (domainNode <- node.getNode("domains").getNodes)
    yield new Domain(
        domainNode.getNode("id"),
        domainNode.getNode("name"),
        domainNode.getNode("key"),
        DomainMode.byName(domainNode.getNode("mode")),
        if(domainNode.getNode("deleted") != null) domainNode.getNode("deleted") else false
      )

    val defaultDomainNode = node.getNode("defaultDomainId")

    val defaultDomain = if (defaultDomainNode != null){
      defaultDomainNode.asInstanceOf[ScalarNode].getValue[String]
    }else{
      domains.filter(d => d.name == "anonymous").head.id
    }

    DomainConfig(domains.toList, defaultDomain)
  }

  def writeConfig(config: DomainConfig, writer: JSONWriter) {

    writer.`object`

    writer.key("defaultDomainId")
    writer.value(config.defaultDomain)

    writer.key("domains")

    writer.array

    for (domain <- config.domains) {
      writer.`object`

      writer.key("id")
      writer.value(domain.id)
      writer.key("name")
      writer.value(domain.name)
      writer.key("key")
      writer.value(domain.key)
      writer.key("mode")
      writer.value(domain.mode.name)
      writer.key("deleted")
      writer.value(domain.deleted)
      writer.endObject
    }
    writer.endArray
    writer.endObject
  }
}