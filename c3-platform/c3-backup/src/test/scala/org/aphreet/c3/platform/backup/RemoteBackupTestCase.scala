package org.aphreet.c3.platform.backup

import impl.{BackupConfigAccessor, RemoteBackup}
import org.aphreet.c3.platform.test.integration.AbstractTestWithFileSystem
import org.aphreet.c3.platform.common.Path
import junit.framework.Assert._
import org.aphreet.c3.platform.resource.{StringDataStream, ResourceVersion, Resource}

/**
 * Created with IntelliJ IDEA.
 * User: antey
 * Date: 02.02.13
 * Time: 0:31
 * To change this template use File | Settings | File Templates.
 */
class RemoteBackupTestCase extends AbstractTestWithFileSystem{

  val BACKUP_NAME = "backup.zip";
  val PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" +
              "MIIEpQIBAAKCAQEA8zU/ebn6gxaYV3QaxaEEnjhBHaXfT8w1NEghPkjerCJFjIje\n" +
              "a45zJRhuxORMVz7zaW1h9l6dPVdhSv7R59FFWLpM/I7MuDWBpdB66p7g0pN43qYH\n" +
              "xCxthgsCqSv9pPIQPpOe/q2NV18o/JpDrySUnK0kEhJHAx3IJypImywb2slYlJ82\n" +
              "jWAhTdBsw5jvy/v9gaLS6K4/53FjIs99MQ+3R4cMF0p+kausiNX4X4HXubPNSFF2\n" +
              "GlCQvU4rsi/USYD6Ht/0YobAaxhdItxTjE3lJ2bjksBYCB9I2sTRwHSvnlvJWdnx\n" +
              "RqhJo8wYm7uJXDM59z7EA7lS6zrwrMEuMP/qOwIDAQABAoIBAQDZbXtH+epqE2My\n" +
              "nAvrLt6QDL66ILaaAnh2Ox3tLvxTa3g+AYbHJVzvhv5Qa8GMJi06zZ1Mwp1UX7AN\n" +
              "ee5yjvauMi/tgkSnUr6LXOzmoA9icpEWi9xZl1Z7BZDlaPyj3/yB1TvJd6Lqv5pG\n" +
              "6iskg1GRGIEs3sY5lQCBcx3iC/cDCkw7lyu7TnzLqQbCqhwbWsCnW0Ip7+EqYXd6\n" +
              "AwsswhREF6XGWcdwtTZa4fRyd7uko4TTmQ9qdA3yPsTe9Us/DgUciAg9rDy3mISb\n" +
              "sjwLOTGwIUhJaePYf26snCspyP9TL/OXVGNDnNBOAlVAglgUQ22zhDrh7jQc9F8C\n" +
              "EZlknnSxAoGBAP4NGLBZoRSBpDY185D81Rp9JBgrdJA1mfu7/7UrNNS4k6KHGwRE\n" +
              "O2+wJYJkbfmTjN9TecwaiqmMF4m2n4dClvR81UZ2fZCexWXO7z5ipLOhlI/iPona\n" +
              "TOW8XuGEPKi/VD4Kk5B/Lth0t3FIW2F75hH+0TMULd1Hwlwbo5JfDyC9AoGBAPUS\n" +
              "25kuNHrniMn59VE5DWeb+M+jC0Ux8qkXtyjFEdIYDXRJHZlrOcy6haBwsxqO2GNz\n" +
              "thXoF/CwnJdHajDIMyNOoneTztY1tRMUu6Gsrvc62R10QMQ7j+yfM+2JAUqjkxbG\n" +
              "CuZgVwnK96lv0CX1siP/uaBBAscGkwzM9KBl8JJXAoGAdecAET/XeNvdqOcK/bvI\n" +
              "7r0rFih8tTybFPAf8SV3JE3w/lmzWRyNdP0Lu2L+gvhORYrJGMcsmRkSB3CAwtZN\n" +
              "lnOky0nMZmS4+dG6yHohM9iGSOxX18Q/MdNlOUYUnMAGybBA4auUwxIP3HwXJLvK\n" +
              "f4mB7zGrQ2m9MuAE3rZNIAUCgYEA2JUn2UIKQyvnqYfrkzJ2dEBVLTsMNMSljWZW\n" +
              "CgPPcfqruT78l585X4LfoSC4SBpBhfK20rlgnueWG+OTJzVmbCeMUV0hCBJCynAi\n" +
              "OglgP0GUqwEYU9PHp/gybhQMPig9T30KGPq/MPpc0TLtov51xnazV7rcd4OJ5WAk\n" +
              "OLSeXNUCgYEAiqvHEMU5x9RnXFzRPqKt2+ZvBDRUi7M7vIbYKvB6ctBuUX+V8Ts2\n" +
              "XKmZqsfogatjxm+7oaXAfZSi2dwmzffSg5Vmhn3nmh/bKIMbLA6Ns133toCqw9Dj\n" +
              "mhpYWeCmHcZnYwnz3ExWJwPMCgrwX4Xjn7GA4JHvHctDBtq2uWd8UWw=\n" +
              "-----END RSA PRIVATE KEY-----";


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

    val backupLocation = new RemoteBackupLocation("backup-c3backup.rhcloud.com",
      "d22b442f096243d499120ff44adfc76a", "app-root/data", PRIVATE_KEY)

    val backup = RemoteBackup.create(BACKUP_NAME, backupLocation)

    resourceList.foreach(backup.addResource(_))

    val fsRoots = Map("domain1" -> "rZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f48-12341234",
                      "domain2" -> "uZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f47-12341234")

    backup.writeFileSystemRoots(fsRoots)

    backup.close()

    val backup1 = RemoteBackup.open(BACKUP_NAME, backupLocation)

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
