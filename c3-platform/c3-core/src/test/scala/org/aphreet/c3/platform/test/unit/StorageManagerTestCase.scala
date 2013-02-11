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

package org.aphreet.c3.platform.test.unit

import junit.framework.TestCase
import junit.framework.Assert._
import org.easymock.EasyMock._
import org.aphreet.c3.platform.storage.impl.StorageManagerImpl
import org.aphreet.c3.platform.storage.dispatcher.StorageDispatcher
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.config.PlatformConfigManager
import org.aphreet.c3.platform.common.{Constants, Path}
import collection.mutable
import org.aphreet.c3.platform.storage.StorageParams
import org.aphreet.c3.platform.storage.RW
import org.aphreet.c3.platform.mock.StorageMock
import org.aphreet.c3.platform.task.TaskManager

class StorageManagerTestCase extends TestCase{

  val storagePath = "/path/to/storage"
  val storageId = "1234"
  val storageName = "StorageMock"

  def testRegisterFactory() {

    val storageManager = new StorageManagerImpl

    val storageParams = StorageParams(storageId, new Path(storagePath), storageName, RW(""), List(), new mutable.HashMap[String, String])

    val taskManager = createMock(classOf[TaskManager])
    expect(taskManager.submitTask(anyObject())).andReturn("capacityTaskId")
    replay(taskManager)

    val storageDispatcher = createMock(classOf[StorageDispatcher])
    expect(storageDispatcher.setStorageParams(List(storageParams)))
    replay(storageDispatcher)


    val storageFactory = createMock(classOf[StorageFactory])
    expect(storageFactory.name).andReturn("StorageMock").anyTimes
    expect(storageFactory.createStorage(
      storageParams, "12341234", storageManager)
    ).andReturn(StorageMock(storageId, storagePath))
    replay(storageFactory)

    val configAccessor = createMock(classOf[StorageConfigAccessor])
    expect(configAccessor.load).andReturn(
      List(storageParams)
    ).atLeastOnce
    replay(configAccessor)

    val configManager = createMock(classOf[PlatformConfigManager])
    expect(configManager.getPlatformProperties).andReturn(
      Map(Constants.C3_SYSTEM_ID -> "12341234")
    ).atLeastOnce
    replay(configManager)

    storageManager.taskManager = taskManager
    storageManager.configAccessor = configAccessor
    storageManager.storageDispatcher = storageDispatcher
    storageManager.platformConfigManager = configManager

    storageManager.init()


    storageManager.registerFactory(storageFactory)

    assertEquals(storageManager.storageForId(storageId), StorageMock(storageId, storagePath))

    verify(storageFactory)
    verify(configAccessor)
    verify(storageDispatcher)
    verify(configManager)
    verify(taskManager)
  }

  def testUpdateStorageMode() {

    val storageManager = new StorageManagerImpl

    val storageParams = StorageParams(storageId, new Path(storagePath), storageName, RW(""), List(), new mutable.HashMap[String, String])
    val updatedParams = StorageParams(storageId, new Path(storagePath), storageName, RO("USER"), List(), new mutable.HashMap[String, String])

    val taskManager = createMock(classOf[TaskManager])
    expect(taskManager.submitTask(notNull())).andReturn("capacityTaskId")
    replay(taskManager)

    val storageDispatcher = createMock(classOf[StorageDispatcher])
    expect(storageDispatcher.setStorageParams(List(storageParams)))
    expect(storageDispatcher.setStorageParams(List(updatedParams)))
    replay(storageDispatcher)


    val storageFactory = createMock(classOf[StorageFactory])
    expect(storageFactory.name).andReturn("StorageMock").anyTimes
    expect(storageFactory.createStorage(
      storageParams, "12341234", storageManager)
    ).andReturn(StorageMock(storageId, storagePath))
    replay(storageFactory)

    val configAccessor = createMock(classOf[StorageConfigAccessor])
    expect(configAccessor.load).andReturn(
      List(storageParams)
    ).times(2)
    expect(configAccessor.store(List(updatedParams))).once()
    expect(configAccessor.load).andReturn(
      List(updatedParams)
    ).times(2)

    replay(configAccessor)

    val configManager = createMock(classOf[PlatformConfigManager])
    expect(configManager.getPlatformProperties).andReturn(
      Map(Constants.C3_SYSTEM_ID -> "12341234")
    ).atLeastOnce
    replay(configManager)


    storageManager.taskManager = taskManager
    storageManager.configAccessor = configAccessor
    storageManager.storageDispatcher = storageDispatcher
    storageManager.platformConfigManager = configManager

    storageManager.init()


    storageManager.registerFactory(storageFactory)

    assertEquals(storageManager.storageForId(storageId), StorageMock(storageId, storagePath))

    storageManager.setStorageMode(storageId, RO("USER"))


    assertEquals(RO("USER"), storageManager.storageForId(storageId).mode)

    verify(storageFactory)
    verify(configAccessor)
    verify(storageDispatcher)
    verify(configManager)
    verify(taskManager)
  }



}