package org.aphreet.c3.platform.backup

import impl.RemoteBackup
import org.aphreet.c3.platform.test.integration.AbstractTestWithFileSystem
import junit.framework.Assert._
import org.aphreet.c3.platform.resource.{StringDataStream, ResourceVersion, Resource}
import java.io.File
import ssh.SSHServerMock
import org.apache.commons.logging.LogFactory

/**
 *
 * User: antey
 * Date: 02.02.13
 * Time: 0:31
 */
class RemoteBackupTestCase extends AbstractTestWithFileSystem{

  val BACKUP_NAME = "backup.zip"
  val LOCALHOST = "127.0.0.1"
  val LOCAL_PORT = 2022
  val USER = System.getProperty("user.name")
  val PASSWORD = ""

  var server : SSHServerMock = null
  val log = LogFactory getLog getClass


  def testResourceCreate(){
    var resourceList = List(
      createResource("rZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f47-12341234"),
      createResource("rZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f48-12341234"),
      createResource("lZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f49-12341234"),
      createResource("aZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f47-12341234"),
      createResource("uZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f47-12341234"),
      createResource("pZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f47-12341234"),
      createResource("dZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f47-12341234")
    )

    val backupLocation = new BackupLocation("", "remote", LOCALHOST, USER, "backups", "", null)

    val backup = RemoteBackup.create(BACKUP_NAME, backupLocation, LOCAL_PORT, PASSWORD)

    resourceList.foreach(backup.addResource(_))

    val fsRoots = Map("domain1" -> "rZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f48-12341234",
                      "domain2" -> "uZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f47-12341234")

    backup.writeFileSystemRoots(fsRoots)

    backup.close()

    val backup1 = RemoteBackup.open(BACKUP_NAME, backupLocation, LOCAL_PORT, PASSWORD)

    for(resource <- backup1){
      assertEquals(1, resourceList.filter(_.address == resource.address).size)
      resourceList = resourceList.filterNot(_.address == resource.address)
    }

    assertEquals(0, resourceList.size)

    assertEquals(fsRoots, backup1.readFileSystemRoots)

    backup1.close()
  }

  def createResource(name:String):Resource = {
    val resource = new Resource
    resource.isVersioned = true
    resource.address = name

    val version = new ResourceVersion()
    version.data = new StringDataStream("Hello, world!")
    resource.addVersion(version)

    val version2 = new ResourceVersion()
    version2.data = new StringDataStream("Hello, world!")
    resource.addVersion(version2)

    resource
  }

  override def setUp(){
  	super.setUp()

    server = new SSHServerMock()
    server.start

    Thread.sleep(2000)
  }

  override def tearDown() {
    if (server != null) {
      server.stop
    }

    super.tearDown()
  }
}
