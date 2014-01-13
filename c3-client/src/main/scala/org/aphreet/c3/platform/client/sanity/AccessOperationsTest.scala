package org.aphreet.c3.platform.client.sanity

import org.junit.Test
import org.junit.Assert._

/**
 * Author: Mikhail Malygin
 * Date:   1/10/14
 * Time:   11:51 PM
 */
class AccessOperationsTest extends AccessOperations{

  @Test
  def testReadWrite(){

    val objects = write(100, 10240)

    readStrict(objects)
  }

  @Test
  def testReadWriteDelete(){

    val objectsToWrite = 100

    val objects = write(objectsToWrite, 10240)

    readStrict(objects)

    assertEquals(objectsToWrite, delete(objects))

    assertEquals(0, read(objects))
  }

  @Test
  def testVersionSelector(){

    val system = createSystem
    
    val objects = write(1, 5120, "application/x-c3-ref") //type to avoid version tracking
    readStrict(objects)
    assertFalse(system.getResource(objects.head).tracksVersions)
    delete(objects)

    

    val versionedObjects = write(1, 10241)
    readStrict(versionedObjects)
    assertTrue(system.getResource(versionedObjects.head).tracksVersions)
    delete(versionedObjects)
  }
  
}
