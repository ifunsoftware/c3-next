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
package org.aphreet.c3.platform.remote.test.replication

import junit.framework.TestCase
import junit.framework.Assert._
import java.io.File
import org.aphreet.c3.platform.config.impl.PlatformConfigManagerImpl
import org.aphreet.c3.platform.remote.replication.impl.config.ReplicationSourcesConfigAccessor
import org.aphreet.c3.platform.remote.api.management.ReplicationHost

class ReplicationConfigAccessorTestCase extends TestCase {

  var testDir:File = null

  override def setUp{
    testDir = new File(System.getProperty("user.home"), "c3_int_test")
    testDir.mkdirs
  }

  override def tearDown{
    def delDir(directory:File) {
      if(directory.isDirectory) directory.listFiles.foreach(delDir(_))
      directory.delete
    }
    delDir(testDir)
  }

  def testConfigPersistence = {

    val config = Map("localhost" -> new ReplicationHost("localhost", "localhost.localdomain", "key1", 7373, 7374, 7375, "user1", "password1"),
                     "darkstar" ->  new ReplicationHost("darkstar", "darkstar.localdomain", "key2", 7373, 7374, 7375, "user2", "password2"))

    val configManager = new PlatformConfigManagerImpl
    configManager.configDir = testDir

    val accessor = new ReplicationSourcesConfigAccessor
    accessor.setConfigManager(configManager)

    val fileName = "c3-replication-sources.json"

    accessor.storeConfig(config, new File(testDir, fileName))

    val readConfig = accessor.loadConfig(new File(testDir, fileName))
    
    assertEquals(config, readConfig)

  }
}