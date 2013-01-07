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
import org.aphreet.c3.platform.storage._

import impl.{StorageConfigAccessorImpl, StorageIndexConfigAccessorImpl}
import org.aphreet.c3.platform.test.integration.AbstractTestWithFileSystem

import junit.framework.Assert._
import org.easymock.EasyMock._
import org.aphreet.c3.platform.config.impl.PlatformConfigManagerImpl
import java.io.File
import collection.mutable

class StorageConfigTest extends AbstractTestWithFileSystem{


  def testConfigPersistence() {

    val paramsMap1 = new mutable.HashMap[String, String]
    val paramsMap2 = new mutable.HashMap[String, String]

    for (i <- 0 to 2) {
      paramsMap1.put("nodeName-" + i, "testName-" + i)
      paramsMap1.put("nodePort-" + i, (10000 * (i+1)).toString)
      paramsMap1.put("nodeDir-" + i, "testdir/" + i)

      paramsMap2.put("nodeName-" + i, "anotherTestName-" + i)
      paramsMap2.put("nodePort-" + i, (20000 * (i+1)).toString)
      paramsMap2.put("nodeDir-" + i, "anotherTestdir/" + i)
    }

    paramsMap1.put("nodeCounter", "5")
    paramsMap1.put("nodeCounter", "3")

    val config  = List(
      StorageParams("11", new Path("C:\\data\\file\\"), "PureBDBStorage", RW("migration"),
        List(), paramsMap1),
      StorageParams("22", new Path("C:\\data\\file1\\"), "FileBDBStorage", RO(""), List(),
        paramsMap2)
    )

    val configManager = new PlatformConfigManagerImpl
    configManager.configDir = testDir

    val indexConfigAccessor = createMock(classOf[StorageIndexConfigAccessor])
    expect(indexConfigAccessor.load).andReturn(List()).atLeastOnce()
    replay(indexConfigAccessor)

    val accessor = new StorageConfigAccessorImpl
    accessor.configManager = configManager
    accessor.indexesConfig = indexConfigAccessor

    val fileName = "c3-storage-config.json"

    accessor.storeConfig(config, new File(testDir, fileName))

    val readConfig = accessor.loadConfig(new File(testDir, fileName))

    assertEquals(config, readConfig)

    val newParams = StorageParams("22", new Path("C:\\data\\file1\\"), "FileBDBStorage", RW(""),
      List(),
      new mutable.HashMap[String, String]
    )

    accessor.update(config => newParams :: config.filter(_.id != newParams.id))

    assertEquals(newParams, accessor.load.head)

    verify(indexConfigAccessor)
  }

  def testIdCheck() {
    val paramsMap1 = new mutable.HashMap[String, String]
    val paramsMap2 = new mutable.HashMap[String, String]

    for (i <- 0 to 2) {
      paramsMap1.put("nodeName-" + i, "testName-" + i)
      paramsMap1.put("nodePort-" + i, (10000 * (i+1)).toString)
      paramsMap1.put("nodeDir-" + i, "testdir/" + i)

      paramsMap2.put("nodeName-" + i, "anotherTestName-" + i)
      paramsMap2.put("nodePort-" + i, (20000 * (i+1)).toString)
      paramsMap2.put("nodeDir-" + i, "anotherTestdir/" + i)
    }

    paramsMap1.put("nodeCounter", "5")
    paramsMap1.put("nodeCounter", "3")

    val config  = List(
      StorageParams("11", new Path("C:\\data\\file\\"), "PureBDBStorage", RW("migration"),
        List(), paramsMap1),
      StorageParams("22", new Path("C:\\data\\file1\\"), "FileBDBStorage", RO(""),
        List(), paramsMap2)
    )

    val idNotExists = config
      .filter(p => p.id == "55").isEmpty

    assertTrue(idNotExists)

    assertFalse(config
      .filter(p => p.id == "11").isEmpty)

    assertTrue(config
      .filter(p => p.id == "33").isEmpty)

  }

  def testIndexConfigPersistence(){
    val configManager = new PlatformConfigManagerImpl
    configManager.configDir = testDir

    val configAccessor = new StorageIndexConfigAccessorImpl
    configAccessor.configManager = configManager

    val indexesConfig =  List(
      new StorageIndex("poolindex", List("c3.pool", "c3.tags"), false, true, 10002l),
      new StorageIndex("tagindex", List("c3.tags"), true, false, 10001l)
    )

    configAccessor.store(indexesConfig)

    assertEquals(indexesConfig, configAccessor.load)
  }
}
