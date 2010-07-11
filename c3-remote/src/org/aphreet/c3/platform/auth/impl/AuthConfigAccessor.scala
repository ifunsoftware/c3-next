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

package org.aphreet.c3.platform.auth.impl

import org.aphreet.c3.platform.config.ConfigAccessor
import javax.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.config.PlatformConfigManager
import java.io.{FileWriter, StringWriter, File}
import org.aphreet.c3.platform.common.{JSONFormatter, Constants}
import com.springsource.json.writer.JSONWriterImpl
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope
import com.springsource.json.parser.{ListNode, ScalarNode, MapNode, AntlrJSONParser}
import collection.mutable.HashMap
import collection.Map
import collection.jcl.Buffer
import org.aphreet.c3.platform.auth.{MANAGEMENT, UserRole, User}

@Component
@Scope("singleton")
class AuthConfigAccessor extends ConfigAccessor[Map[String, User]] {
  var configManager: PlatformConfigManager = _

  def configDir: File = configManager.configDir

  def configFileName: String = "c3-auth-config.json"

  @Autowired
  def setConfigManager(manager: PlatformConfigManager) = {configManager = manager}

  def defaultConfig:Map[String, User] = {
    val map = new HashMap[String, User]

    map.put("admin", User("admin", "5f4dcc3b5aa765d61d8327deb882cf99", MANAGEMENT))

    map
  }

  def loadConfig(configFile: File): Map[String, User] = {
    val map = new HashMap[String, User]


    val node = new AntlrJSONParser().parse(configFile).asInstanceOf[MapNode]

    val userListNode = node.getNode("users").asInstanceOf[ListNode]

    for (userNode <- Buffer.apply(userListNode.getNodes)) {
      val name = getValue(userNode.asInstanceOf[MapNode], "name")
      val password = getValue(userNode.asInstanceOf[MapNode], "password")
      val role = getValue(userNode.asInstanceOf[MapNode], "role")

      val user = new User(name, password, UserRole.fromString(role))

      map.put(name, user)
    }
    map
  }

  def storeConfig(map: Map[String, User], configFile: File) = {
    this.synchronized {
      val swriter = new StringWriter()

      var fileWriter: FileWriter = null

      try {
        val writer = new JSONWriterImpl(swriter)

        writer.`object`

        writer.key("users")

        writer.array

        for ((name, user) <- map) {
          writer.`object`

          writer.key("name")
          writer.value(user.name)
          writer.key("password")
          writer.value(user.password)
          writer.key("role")
          writer.value(user.role.name)
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