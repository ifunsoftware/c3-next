package org.aphreet.c3.platform.filesystem.test

import org.aphreet.c3.platform.filesystem.FSCleanupManager
import org.easymock.EasyMock
import EasyMock._
import org.aphreet.c3.platform.access.{AccessMediator, AccessManager}
import org.aphreet.c3.platform.storage.StorageManager
import junit.framework.TestCase
import org.aphreet.c3.platform.filesystem.FSCleanupManagerProtocol.CleanupDirectoryTask

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */
class FSCleanupManagerTestCase extends TestCase with FSTestHelpers{
  val fsCleanupManager = new FSCleanupManager

  def testCleanupTask(){

    val accessManager = createMock(classOf[AccessManager])

    expect(accessManager.get("00000000-c2fd-4bef-936e-59cef7943840-6a01")).andReturn(resourceStub("root", "00000001-c2fd-4bef-936e-59cef7943840-6a00")).anyTimes
    expect(accessManager.get("00000001-c2fd-4bef-936e-59cef7943840-6a02")).andReturn(resourceStub("dir11", "00000002-c2fd-4bef-936e-59cef7943840-6a00")).anyTimes
    expect(accessManager.get("00000002-c2fd-4bef-936e-59cef7943840-6a03")).andReturn(resourceStub("dir12", "00000003-c2fd-4bef-936e-59cef7943840-6a00")).anyTimes
    expect(accessManager.get("00000003-c2fd-4bef-936e-59cef7943840-6a04")).andReturn(resourceStub("file21", "00000003-c2fd-4bef-936e-59cef7943840-6a00")).anyTimes
    replay(accessManager)

    val storageManager = createMock(classOf[StorageManager])
    val accessMediator = createMock(classOf[AccessMediator])

    fsCleanupManager.accessManager = accessManager
    fsCleanupManager.storageManager = storageManager
    fsCleanupManager.accessMediator = accessMediator

    val rootDirStub = directoryStub(resourceStub("rootDir", "00000000-c2fd-4bef-936e-59cef7943840-6a01"))
    rootDirStub.addChild("dir11", "00000002-c2fd-4bef-936e-59cef7943840-6a02", false)

    fsCleanupManager ! CleanupDirectoryTask(rootDirStub)

    // TODO
  }
}