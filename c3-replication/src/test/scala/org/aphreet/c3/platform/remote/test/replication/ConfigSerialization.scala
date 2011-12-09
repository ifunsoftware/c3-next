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

package org.aphreet.c3.platform.remote.test.replication

import com.thoughtworks.xstream.io.xml.DomDriver
import com.thoughtworks.xstream.XStream
import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.remote.replication.impl.config._
import org.aphreet.c3.platform.remote.api.management.{ReplicationHost, StorageDescription, Pair, DomainDescription}

class ConfigSerialization extends TestCase{

  def testPlatformConfig = {

    val platformConfig = PlatformInfo("systemId",
      ReplicationHost("localhost", "localhost.localdomain", "key1", 7373, 7374, 7375, "my_encoded_aes_key"),
      Array(new StorageDescription("1111", Array("2222", "3333"), "PureBDBStorage", "path", "RO", 0, Array()),
            new StorageDescription("4444", Array("5555", "6666"), "FileBDBStorage", "path", "RW", 0, Array())),
      Array(new DomainDescription("id", "name", "key", "full"),
            new DomainDescription("id2", "name2", "key2", "full")),
      Array(new Pair("sdsd", "sfdsfdsfsdf"),
            new Pair("sds", "asdsad")))

    val xStream = new XStream(new DomDriver("UTF-8"))

    val output = xStream.toXML(platformConfig)

    val platformConfig2 = xStream.fromXML(output).asInstanceOf[PlatformInfo]

    val output2 = xStream.toXML(platformConfig2)

    println(output)

    assertEquals(output, output2)
  }
}