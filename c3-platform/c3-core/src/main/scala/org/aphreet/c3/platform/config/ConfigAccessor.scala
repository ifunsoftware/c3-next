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
package org.aphreet.c3.platform.config

import java.io.{StringWriter, File}
import org.aphreet.c3.platform.common.Disposable._
import java.nio.file.{StandardOpenOption, Files}
import com.springsource.json.parser._
import com.springsource.json.writer._


trait ConfigAccessor[T] {
  def load: T = {

    val file = new File(configDir, configFileName)

    if (!file.exists) {
      storeConfig(defaultConfig, file)
    }

    loadConfig(file)
  }

  def store(data: T) {
    storeConfig(data, new File(configDir, configFileName))
  }

  def update(f: (T) => T) {
    store(f.apply(load))
  }

  def storeConfig(data: T, configFile: File) {
    this.synchronized {
      using(new StringWriter())(
        writer => {
          writeConfig(data, new JSONWriterImpl(writer))
          Files.write(configFile.toPath, writer.toString.getBytes("UTF-8"),
            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        }
      )
    }
  }

  def loadConfig(configFile: File): T = {
    readConfig(new AntlrJSONParser().parse(configFile))
  }

  implicit def asMap(node:Node):MapNode = node.asInstanceOf[MapNode]

  implicit def asList(node:Node):ListNode = node.asInstanceOf[ListNode]

  implicit def asScalar(node:Node):ScalarNode = node.asInstanceOf[ScalarNode]

  implicit def asStringScalarNodeValue(node:Node):String = node.asInstanceOf[ScalarNode].getValue[String]

  implicit def asBooleanScalarNodeValue(node:Node):Boolean = node.asInstanceOf[ScalarNode].getValue[Boolean]


  protected def configDir: File

  protected def configFileName: String

  protected def defaultConfig: T

  def writeConfig(data: T, writer: JSONWriter)

  def readConfig(node:Node):T

}
