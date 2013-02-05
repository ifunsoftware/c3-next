package org.aphreet.c3.platform.backup

import junit.framework.Assert._
import org.aphreet.c3.platform.backup.ResourceConsumer
import impl.Backup
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.resource.{StringDataStream, ResourceVersion, Resource}
import org.aphreet.c3.platform.test.integration.AbstractTestWithFileSystem


class BackupTestCase extends AbstractTestWithFileSystem{

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

    val backup = Backup.create(new Path(testDir.getAbsolutePath + "/backup.zip"))

    resourceList.foreach(backup.addResource(_))

    val fsRoots = Map("domain1" -> "rZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f48-12341234",
                      "domain2" -> "uZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f47-12341234")

    backup.writeFileSystemRoots(fsRoots)

    backup.close()

    val backup1 = Backup.open(new Path(testDir.getAbsolutePath + "/backup.zip"))

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
}
