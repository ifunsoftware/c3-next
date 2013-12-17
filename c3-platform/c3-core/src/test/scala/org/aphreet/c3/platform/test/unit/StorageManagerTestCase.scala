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

import collection.mutable
import junit.framework.Assert._
import junit.framework.TestCase
import org.aphreet.c3.platform.common.{Constants, Path}
import org.aphreet.c3.platform.config.impl.MemoryConfigPersister
import org.aphreet.c3.platform.config.{ConfigPersister, PlatformConfigComponent, PlatformConfigManager}
import org.aphreet.c3.platform.mock.StorageMock
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.storage.dispatcher.{StorageDispatcherComponent, StorageDispatcher}
import org.aphreet.c3.platform.storage.impl.{StorageIndexConfigAccessorImpl, StorageConfigAccessorImpl, StorageComponentImpl}
import org.aphreet.c3.platform.task.{TaskComponent, TaskManager}
import org.easymock.EasyMock._

class StorageManagerTestCase extends TestCase {

  val storagePath = "/path/to/storage"
  val storageId = "1234"
  val storageName = "StorageMock"

  def testRegisterFactory() {

    val testConfigPersister = new MemoryConfigPersister

    val storageParams = StorageParams(storageId, new Path(storagePath), storageName, RW(""), List(), new mutable.HashMap[String, String])

    val createdStorageParams = StorageParams(storageId, new Path(storagePath), storageName, RW(""),
      List(),
      new mutable.HashMap[String, String])

    trait TaskComponentMock extends TaskComponent{
      val taskManager: TaskManager = createMock(classOf[TaskManager])
      expect(taskManager.submitTask(anyObject())).andReturn("capacityTaskId")
      replay(taskManager)
    }
    
    trait StorageDispatcherComponentMock extends StorageDispatcherComponent{
      val storageDispatcher: StorageDispatcher = createMock(classOf[StorageDispatcher])
      expect(storageDispatcher.setStorageParams(List(createdStorageParams)))
      replay(storageDispatcher)  
    }

    trait ConfigComponentMock extends PlatformConfigComponent {
      val platformConfigManager: PlatformConfigManager = createMock(classOf[PlatformConfigManager])
      expect(platformConfigManager.getPlatformProperties).andReturn(
        Map(Constants.C3_SYSTEM_ID -> "12341234")
      ).atLeastOnce
      replay(platformConfigManager)

      val configPersister: ConfigPersister = testConfigPersister
    }

    new StorageIndexConfigAccessorImpl(testConfigPersister).store(List())

    val configAccessor = new StorageConfigAccessorImpl(testConfigPersister)
    configAccessor.store(List(storageParams))

    val app = new Object
      with StorageDispatcherComponentMock
      with ConfigComponentMock
      with TaskComponentMock
      with StorageComponentImpl

    val storageManager = app.storageManager

    val storageFactory = createMock(classOf[StorageFactory])
    expect(storageFactory.name).andReturn("StorageMock").anyTimes
    expect(storageFactory.createStorage(
      createdStorageParams, "12341234", storageManager.asInstanceOf[ConflictResolverProvider])
    ).andReturn(StorageMock(storageId, storagePath))
    replay(storageFactory)


    storageManager.registerFactory(storageFactory)

    assertEquals(storageManager.storageForId(storageId), Some(StorageMock(storageId, storagePath)))

    verify(storageFactory)
    verify(app.storageDispatcher)
    verify(app.platformConfigManager)
    verify(app.taskManager)
  }

  def testUpdateStorageMode() {

    val testConfigPersister = new MemoryConfigPersister

    val storageParams = StorageParams(storageId, new Path(storagePath), storageName, RW(""),
      List(),
      new mutable.HashMap[String, String])


    val updatedParams = StorageParams(storageId, new Path(storagePath), storageName, RO("USER"),
      List(),
      new mutable.HashMap[String, String])

    val configAccessor = new StorageConfigAccessorImpl(testConfigPersister)
    configAccessor.store(List(storageParams))

    trait TaskComponentMock extends TaskComponent{
      val taskManager: TaskManager = createMock(classOf[TaskManager])
      expect(taskManager.submitTask(notNull())).andReturn("capacityTaskId")
      replay(taskManager)
    }

    trait StorageDispatcherComponentMock extends StorageDispatcherComponent {
      val storageDispatcher: StorageDispatcher = createMock(classOf[StorageDispatcher])
      expect(storageDispatcher.setStorageParams(List(storageParams)))
      expect(storageDispatcher.setStorageParams(List(updatedParams)))
      replay(storageDispatcher)
    }

    trait ConfigComponentMock extends PlatformConfigComponent {
      val platformConfigManager = createMock(classOf[PlatformConfigManager])
      expect(platformConfigManager.getPlatformProperties).andReturn(
        Map(Constants.C3_SYSTEM_ID -> "12341234")
      ).atLeastOnce
      replay(platformConfigManager)

      val configPersister = testConfigPersister
    }

    new StorageIndexConfigAccessorImpl(testConfigPersister).store(List())

    val app = new Object
      with StorageDispatcherComponentMock
      with ConfigComponentMock
      with TaskComponentMock
      with StorageComponentImpl

    val storageManager = app.storageManager

    val storageFactory = createMock(classOf[StorageFactory])
    expect(storageFactory.name).andReturn("StorageMock").anyTimes
    expect(storageFactory.createStorage(
      storageParams, "12341234", storageManager.asInstanceOf[ConflictResolverProvider])
    ).andReturn(StorageMock(storageId, storagePath))
    replay(storageFactory)

    storageManager.registerFactory(storageFactory)

    assertEquals(storageManager.storageForId(storageId), Some(StorageMock(storageId, storagePath)))

    storageManager.setStorageMode(storageId, RO("USER"))


    assertEquals(RO("USER"), storageManager.storageForId(storageId).get.mode)

    verify(storageFactory)
    verify(app.storageDispatcher)
    verify(app.platformConfigManager)
    verify(app.taskManager)
  }



}