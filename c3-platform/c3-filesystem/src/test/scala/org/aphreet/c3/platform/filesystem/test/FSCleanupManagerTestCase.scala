package org.aphreet.c3.platform.filesystem.test

import org.easymock.EasyMock
import EasyMock._
import org.aphreet.c3.platform.access.{ResourceDeletedMsg, AccessComponent, AccessMediator, AccessManager}
import org.aphreet.c3.platform.storage.{StorageComponent, StorageLike, StorageManager}
import junit.framework.TestCase
import org.aphreet.c3.platform.resource.ResourceAddress
import org.aphreet.c3.platform.filesystem.impl.FSCleanupComponentImpl
import org.aphreet.c3.platform.common.DefaultComponentLifecycle

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */
class FSCleanupManagerTestCase extends TestCase with FSTestHelpers{

  def testCleanupTask(){

    val storageLike = createMock(classOf[StorageLike])
    expect(storageLike.delete("00000000-c2fd-4bef-936e-59cef7943840-6a01")).andReturn("00000000-c2fd-4bef-936e-59cef7943840-6a01").once()
    expect(storageLike.delete("00000000-c2fd-4bef-936e-59cef7943840-6a02")).andReturn("00000000-c2fd-4bef-936e-59cef7943840-6a02").once()
    expect(storageLike.delete("00000000-c2fd-4bef-936e-59cef7943840-6a03")).andReturn("00000000-c2fd-4bef-936e-59cef7943840-6a03").once()
    expect(storageLike.delete("00000000-c2fd-4bef-936e-59cef7943840-6a04")).andReturn("00000000-c2fd-4bef-936e-59cef7943840-6a04").once()
    replay(storageLike)

    val accessManagerMock = createMock(classOf[AccessManager])
    expect(accessManagerMock.get("00000000-c2fd-4bef-936e-59cef7943840-6a02")).andReturn(directoryStub(resourceStub("dir11", "00000000-c2fd-4bef-936e-59cef7943840-6a01", "00000000-c2fd-4bef-936e-59cef7943840-6a02")).resource).atLeastOnce()
    expect(accessManagerMock.get("00000000-c2fd-4bef-936e-59cef7943840-6a03")).andReturn(directoryStub(resourceStub("dir12", "00000000-c2fd-4bef-936e-59cef7943840-6a01", "00000000-c2fd-4bef-936e-59cef7943840-6a03")).resource).atLeastOnce()
    expect(accessManagerMock.get("00000000-c2fd-4bef-936e-59cef7943840-6a04")).andReturn(fileStub(resourceStub("file21", "00000000-c2fd-4bef-936e-59cef7943840-6a01", "00000000-c2fd-4bef-936e-59cef7943840-6a04")).resource).atLeastOnce()
    replay(accessManagerMock)

    val storageManagerMock = createMock(classOf[StorageManager])
    expect(storageManagerMock.storageForAddress(ResourceAddress("00000000-c2fd-4bef-936e-59cef7943840-6a01"))).andReturn(Some(storageLike)).once()
    expect(storageManagerMock.storageForAddress(ResourceAddress("00000000-c2fd-4bef-936e-59cef7943840-6a02"))).andReturn(Some(storageLike)).once()
    expect(storageManagerMock.storageForAddress(ResourceAddress("00000000-c2fd-4bef-936e-59cef7943840-6a03"))).andReturn(Some(storageLike)).once()
    expect(storageManagerMock.storageForAddress(ResourceAddress("00000000-c2fd-4bef-936e-59cef7943840-6a04"))).andReturn(Some(storageLike)).once()
    replay(storageManagerMock)

    val accessMediatorMock = createMock(classOf[AccessMediator])
    expect(accessMediatorMock.!(ResourceDeletedMsg("00000000-c2fd-4bef-936e-59cef7943840-6a01", 'FSCleanupManager))).once()
    expect(accessMediatorMock.!(ResourceDeletedMsg("00000000-c2fd-4bef-936e-59cef7943840-6a02", 'FSCleanupManager))).once()
    expect(accessMediatorMock.!(ResourceDeletedMsg("00000000-c2fd-4bef-936e-59cef7943840-6a03", 'FSCleanupManager))).once()
    expect(accessMediatorMock.!(ResourceDeletedMsg("00000000-c2fd-4bef-936e-59cef7943840-6a04", 'FSCleanupManager))).once()
    replay(accessMediatorMock)

    val app = new Object
      with DefaultComponentLifecycle
      with StorageComponent
      with AccessComponent
      with FSCleanupComponentImpl {
      def accessManager: AccessManager = accessManagerMock

      def accessMediator: AccessMediator = accessMediatorMock

      def storageManager: StorageManager = storageManagerMock
    }


    val rootDirStub = directoryStub(resourceStub("root", "00000000-c2fd-4bef-936e-59cef7943840-6a00", "00000000-c2fd-4bef-936e-59cef7943840-6a01"))
    rootDirStub.addChild("dir11", "00000000-c2fd-4bef-936e-59cef7943840-6a02", leaf = false)
    rootDirStub.addChild("dir12", "00000000-c2fd-4bef-936e-59cef7943840-6a03", leaf = false)
    rootDirStub.addChild("file21", "00000000-c2fd-4bef-936e-59cef7943840-6a04", leaf = true)

    app.filesystemCleanupManager.cleanupDirectory(rootDirStub)

    verify(accessManagerMock)
    verify(accessMediatorMock)
    verify(storageManagerMock)
    verify(storageLike)
  }
}