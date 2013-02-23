package org.aphreet.c3.platform.filesystem.test

import org.easymock.EasyMock
import EasyMock._
import org.aphreet.c3.platform.access.{AccessMediator, AccessManager}
import org.aphreet.c3.platform.storage.{StorageLike, StorageManager}
import junit.framework.TestCase
import org.aphreet.c3.platform.resource.ResourceAddress
import org.aphreet.c3.platform.filesystem.impl.FSCleanupManagerImpl

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */
class FSCleanupManagerTestCase extends TestCase with FSTestHelpers{
  val fsCleanupManager = new FSCleanupManagerImpl

  def testCleanupTask(){

    val storageLike = createMock(classOf[StorageLike])
    expect(storageLike.delete("00000000-c2fd-4bef-936e-59cef7943840-6a01")).once()
    expect(storageLike.delete("00000000-c2fd-4bef-936e-59cef7943840-6a02")).once()
    expect(storageLike.delete("00000000-c2fd-4bef-936e-59cef7943840-6a03")).once()
    expect(storageLike.delete("00000000-c2fd-4bef-936e-59cef7943840-6a04")).once()
    replay(storageLike)

    val accessManager = createMock(classOf[AccessManager])
    expect(accessManager.get("00000000-c2fd-4bef-936e-59cef7943840-6a01")).andReturn(directoryStub(resourceStub("root", "00000000-c2fd-4bef-936e-59cef7943840-6a00", "00000000-c2fd-4bef-936e-59cef7943840-6a01")).resource).atLeastOnce()
    expect(accessManager.get("00000000-c2fd-4bef-936e-59cef7943840-6a02")).andReturn(directoryStub(resourceStub("dir11", "00000000-c2fd-4bef-936e-59cef7943840-6a01", "00000000-c2fd-4bef-936e-59cef7943840-6a02")).resource).atLeastOnce()
    expect(accessManager.get("00000000-c2fd-4bef-936e-59cef7943840-6a03")).andReturn(directoryStub(resourceStub("dir12", "00000000-c2fd-4bef-936e-59cef7943840-6a01", "00000000-c2fd-4bef-936e-59cef7943840-6a03")).resource).atLeastOnce()
    expect(accessManager.get("00000000-c2fd-4bef-936e-59cef7943840-6a04")).andReturn(fileStub(resourceStub("file21", "00000000-c2fd-4bef-936e-59cef7943840-6a01", "00000000-c2fd-4bef-936e-59cef7943840-6a04")).resource).atLeastOnce()
    replay(accessManager)

    val storageManager = createMock(classOf[StorageManager])
    expect(storageManager.storageForAddress(ResourceAddress("00000000-c2fd-4bef-936e-59cef7943840-6a01"))).andReturn(storageLike).once()
    expect(storageManager.storageForAddress(ResourceAddress("00000000-c2fd-4bef-936e-59cef7943840-6a02"))).andReturn(storageLike).once()
    expect(storageManager.storageForAddress(ResourceAddress("00000000-c2fd-4bef-936e-59cef7943840-6a03"))).andReturn(storageLike).once()
    expect(storageManager.storageForAddress(ResourceAddress("00000000-c2fd-4bef-936e-59cef7943840-6a04"))).andReturn(storageLike).once()
    replay(storageManager)

    val accessMediator = createMock(classOf[AccessMediator])

    fsCleanupManager.accessManager = accessManager
    fsCleanupManager.storageManager = storageManager
    fsCleanupManager.accessMediator = accessMediator

    val rootDirStub = directoryStub(resourceStub("root", "00000000-c2fd-4bef-936e-59cef7943840-6a00", "00000000-c2fd-4bef-936e-59cef7943840-6a01"))
    rootDirStub.addChild("dir11", "00000000-c2fd-4bef-936e-59cef7943840-6a02", false)
    rootDirStub.addChild("dir12", "00000000-c2fd-4bef-936e-59cef7943840-6a03", false)
    rootDirStub.addChild("file21", "00000000-c2fd-4bef-936e-59cef7943840-6a03", true)

    fsCleanupManager.cleanupDirectory(rootDirStub)
  }
}