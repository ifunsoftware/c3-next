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


import collection.JavaConversions._
import com.springsource.json.parser._
import com.springsource.json.writer.JSONWriter
import org.aphreet.c3.platform.config.ConfigAccessor


abstract class SelectorConfigAccessor[T] extends ConfigAccessor[Map[T, Boolean]] {

  def defaultConfig:Map[T, Boolean] = Map()

  def readConfig(node:Node): Map[T, Boolean] = {

    val entries =
      for (key <- node.getKeys)
      yield (
        keyFromString(key),
        getArrayValue[Boolean](node, key, 0)
        )
    Map[T, Boolean]() ++ entries

  }

  def keyFromString(string: String): T

  def keyToString(key: T): String

  private def getArrayValue[E](node: MapNode, key: String, num: Int): E = {
    node.getNode(key).getNodes.get(num).getValue[E]
  }

  def writeConfig(data: Map[T, Boolean], writer: JSONWriter) {
    writer.`object`

    for (entry <- data) {
      writer.key(keyToString(entry._1))
      writer.array
      writer.value(entry._2)
      writer.endArray
    }

    writer.endObject
  }
}
