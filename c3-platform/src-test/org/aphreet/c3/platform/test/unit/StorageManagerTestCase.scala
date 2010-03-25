package org.aphreet.c3.platform.test.unit

import junit.framework.TestCase
import org.easymock.classextension.EasyMock._
import org.easymock.classextension.EasyMock
import org.aphreet.c3.platform.storage.impl.{StorageManagerImpl, StorageConfigAccessor}
import org.aphreet.c3.platform.storage.volume.VolumeManager
import org.aphreet.c3.platform.mock.StorageMock
import org.aphreet.c3.platform.storage.dispatcher.StorageDispatcher
import org.aphreet.c3.platform.storage.{StorageFactory, RW, StorageParams}

/**
 * Created by IntelliJ IDEA.
 * User: malygm
 * Date: Mar 25, 2010
 * Time: 3:46:54 PM
 * To change this template use File | Settings | File Templates.
 */

class StorageManagerTestCase extends TestCase{

  val storagePath = "/path/to/storage"
  val storageId = "1234"
  val storageName = "StorageMock"

  def testStorageManager = {
//    val storageManager = new StorageManagerImpl
//
//    val configAccessor = createMock(classOf[StorageConfigAccessor])
//    EasyMock.expect(configAccessor.load).andReturn(
//      List(StorageParams(id, List(), Path(storagePath), storageName, RW))
//    ).atLeastOnce
//    replay(configAccessor)
//
//    val volumeManager = createMock(classOf[VolumeManager])
//    EasyMock.expect(volumeManager.register(StorageMock(storageId, storagePath)))
//    replay(volumeManager)
//
//    val storageDispatcher = createMock(classOf[StorageDispatcher])
//    EasyMock.expect(storageDispatcher.setStorages(List(StorageMock(storageId, storagePath))))
//    replay(storageDispatcher)
//
//    storageManager.setConfigAccessor(configAccessor)
//    storageManager.setVolumeManager(volumeManager)
//    storageManager.setStorageDispatcher(storageDispatcher)
//
//    val storageFactory = createMock(classOf[StorageFactory])
//    EasyMock.expect(storageFactory.name).andReturn("StorageMock").anyTimes
//    EasyMock.expect(storageFactory.createStorage(
//      StorageParams(id, List(), Path(storagePath), storageName, RW))
//    ).andReturn(StorageMock(storageId, storagePath))
//
//    replay(storageFactory)
//
//    storageManager.registerFactory(storageFactory)
//
//    verify(storageFactory)
//    verify(configAccessor)
//    verify(volumeManager)
//    verify(storageDispatcher)
//

  }

}