package org.aphreet.c3.platform.test.unit

import junit.framework.TestCase
import junit.framework.Assert._
import org.easymock.EasyMock._
import org.aphreet.c3.platform.storage.impl.{StorageManagerImpl, StorageConfigAccessorImpl}
import org.aphreet.c3.platform.storage.volume.VolumeManager
import org.aphreet.c3.platform.storage.dispatcher.StorageDispatcher
import org.aphreet.c3.platform.mock.StorageMock
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.storage.{StorageConfigAccessor, StorageFactory, RW, StorageParams}

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
      StorageParams(storageId, List(), new Path(storagePath), storageName, RW("")))
    ).andReturn(StorageMock(storageId, storagePath))
    replay(storageFactory)

    val configAccessor = createMock(classOf[StorageConfigAccessor])
    expect(configAccessor.load).andReturn(
      List(StorageParams(storageId, List(), new Path(storagePath), storageName, RW("")))
    ).atLeastOnce
    replay(configAccessor)


    val storageManager = new StorageManagerImpl
    
    storageManager.setConfigAccessor(configAccessor)
    storageManager.setVolumeManager(volumeManager)
    storageManager.setStorageDispatcher(storageDispatcher)


    storageManager.registerFactory(storageFactory)

    assertEquals(storageManager.storageForId(storageId), StorageMock(storageId, storagePath))

    verify(storageFactory)
    verify(configAccessor)
    verify(volumeManager)
    verify(storageDispatcher)
  }

  

}