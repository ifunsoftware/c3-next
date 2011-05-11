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
package org.aphreet.c3.platform.test.integration.config

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.storage.impl.StorageConfigAccessorImpl
import org.aphreet.c3.platform.storage._

import org.aphreet.c3.platform.test.integration.AbstractTestWithFileSystem

import junit.framework.Assert._
import org.aphreet.c3.platform.config.impl.PlatformConfigManagerImpl
import java.io.File

class StorageConfigTest extends AbstractTestWithFileSystem{


  def testConfigPersistence = {
    
    val config  = List(
      StorageParams("11", List(), new Path("C:\\data\\file\\"), "PureBDBStorage", RW("migration"),
        List(
          new StorageIndex("poolindex", List("c3.pool"), false, false, 10000l)
          )),
      StorageParams("22", List("33","44"), new Path("C:\\data\\file1\\"), "FileBDBStorage", RO(""),
        List(
          new StorageIndex("poolindex", List("c3.pool", "c3.tags"), false, true, 10002l),
          new StorageIndex("tagindex", List("c3.tags"), true, false, 10001l)
        ))
    )
    
    val configManager = new PlatformConfigManagerImpl
    configManager.configDir = testDir
    
    val accessor = new StorageConfigAccessorImpl
    accessor.setConfigManager(configManager)

    val fileName = "c3-storage-config.json"
    
    
    accessor.storeConfig(config, new File(testDir, fileName))
    
    val readConfig = accessor.loadConfig(new File(testDir, fileName))
    
    assertEquals(config, readConfig)
    
    val newParams = StorageParams("22", List("33","44"), new Path("C:\\data\\file1\\"), "FileBDBStorage", RW(""),
      List(
        new StorageIndex("poolindex", List("c3.pool", "c3.tags"), true, true,10003l)
        )
      )
    
    
    accessor.update(config => newParams :: config.filter(_.id != newParams.id))
    
    assertEquals(newParams, accessor.load.head) 
    
  }

  def testIdCheck = {

    val config  = List(
      StorageParams("11", List(), new Path("C:\\data\\file\\"), "PureBDBStorage", RW("migration"),
        List(
          new StorageIndex("poolindex", List("c3.pool"), false, false, 10000l)
          )),
      StorageParams("22", List("33","44"), new Path("C:\\data\\file1\\"), "FileBDBStorage", RO(""),
        List(
          new StorageIndex("poolindex", List("c3.pool", "c3.tags"), false, true, 10002l),
          new StorageIndex("tagindex", List("c3.tags"), true, false, 10001l)
        ))
    )

    val idNotExists = config
            .filter(p => p.id == "55" || p.secIds.contains("55")).isEmpty

    assertTrue(idNotExists)

    assertFalse(config
            .filter(p => p.id == "11" || p.secIds.contains("11")).isEmpty)

    assertFalse(config
                .filter(p => p.id == "33" || p.secIds.contains("33")).isEmpty)

  }
}
