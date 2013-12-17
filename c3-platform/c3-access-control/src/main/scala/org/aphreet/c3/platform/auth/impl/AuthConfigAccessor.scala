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

import collection.JavaConversions._
import scala.collection.{mutable, Map}
import com.springsource.json.parser._
import com.springsource.json.writer.JSONWriter
import org.aphreet.c3.platform.auth.User
import org.aphreet.c3.platform.config.{ConfigPersister, SystemDirectoryProvider, ConfigAccessor}

class AuthConfigAccessor(val persister: ConfigPersister) extends ConfigAccessor[Map[String, User]] {

  def name: String = "c3-auth-config"

  def defaultConfig:Map[String, User] =
    Map("admin" -> User("admin", "password", enabled = true))

  def readConfig(node: Node): Map[String, User] = {
    val map = new mutable.HashMap[String, User]

    for (userNode <- node.getNode("users").getNodes) {

      val name = userNode.getNode("name")
      map.put(name, new User(
        name,
        userNode.getNode("password"),
        userNode.getNode("enabled")))
    }
    map
  }

  def writeConfig(map: Map[String, User], writer: JSONWriter) {
    writer.`object`

    writer.key("users")

    writer.array

    for ((name, user) <- map) {
      writer.`object`

      writer.key("name")
      writer.value(user.name)
      writer.key("password")
      writer.value(user.password)
      writer.key("enabled")
      writer.value(user.enabled)
      writer.endObject
    }
    writer.endArray
    writer.endObject
  }
}