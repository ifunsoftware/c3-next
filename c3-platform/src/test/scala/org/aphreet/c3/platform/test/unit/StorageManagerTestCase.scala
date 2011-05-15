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
import org.aphreet.c3.platform.storage.impl.{StorageManagerImpl, StorageConfigAccessorImpl}
import org.aphreet.c3.platform.storage.volume.VolumeManager
import org.aphreet.c3.platform.storage.dispatcher.StorageDispatcher
import org.aphreet.c3.platform.mock.StorageMock
import org.aphreet.c3.platform.storage.{StorageConfigAccessor, StorageFactory, RW, StorageParams}
import org.aphreet.c3.platform.config.PlatformConfigManager
import org.aphreet.c3.platform.common.{Constants, Path}
import collection.mutable.HashMap

class StorageManagerTestCase extends TestCase{

  val storagePath = "/path/to/storage"
  val storageId = "1234"
  val storageName = "StorageMock"

  def testRegisterFactory = {

    val volumeManager = createMock(classOf[VolumeManager])
    expect(volumeManager.register(StorageMock(storageId, storagePath)))
    replay(volumeManager)

    val storageDispatcher = createMock(classOf[StorageDispatcher])
    expect(storageDispatcher.setStorages(List(StorageMock(storageId, storagePath))))
    replay(storageDispatcher)


    val storageFactory = createMock(classOf[StorageFactory])
    expect(storageFactory.name).andReturn("StorageMock").anyTimes
    expect(storageFactory.createStorage(
      StorageParams(storageId, List(), new Path(storagePath), storageName, RW(""), List(), new HashMap[String, String]), "12341234")
    ).andReturn(StorageMock(storageId, storagePath))
    replay(storageFactory)

    val configAccessor = createMock(classOf[StorageConfigAccessor])
    expect(configAccessor.load).andReturn(
      List(StorageParams(storageId, List(), new Path(storagePath), storageName, RW(""), List(), new HashMap[String, String]))
    ).atLeastOnce
    replay(configAccessor)

    val configManager = createMock(classOf[PlatformConfigManager])
    expect(configManager.getPlatformProperties).andReturn(
        Map(Constants.C3_SYSTEM_ID -> "12341234")
        ).atLeastOnce
    replay(configManager)

    val storageManager = new StorageManagerImpl
    
    storageManager.setConfigAccessor(configAccessor)
    storageManager.setVolumeManager(volumeManager)
    storageManager.setStorageDispatcher(storageDispatcher)
    storageManager.setPlatformConfigManager(configManager)

    storageManager.init


    storageManager.registerFactory(storageFactory)

    assertEquals(storageManager.storageForId(storageId), StorageMock(storageId, storagePath))

    verify(storageFactory)
    verify(configAccessor)
    verify(volumeManager)
    verify(storageDispatcher)
    verify(configManager)
  }

  

}