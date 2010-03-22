package org.aphreet.c3.platform.test.unit

import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.resource.{DataWrapper, ResourceVersion, Resource}
import java.util.Date

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 22, 2010
 * Time: 11:44:13 PM
 * To change this template use File | Settings | File Templates.
 */

class ResourceTestCase extends TestCase {

  val serializedResourceVersion0:Array[Byte] = Array(
    0, 0, 0, 0, 0, 0, 0, 0, 41, 48,
    101, 54, 51, 49, 53, 101, 97, 45, 99, 50,
    102, 100, 45, 52, 98, 101, 102, 45, 57, 51,
    54, 101, 45, 53, 57, 99, 101, 102, 55, 57,
    52, 51, 56, 52, 49, 45, 54, 97, 52, 55,
    0, 0, 0, 0, 0, 0, 39, 16, 0, 0,
    0, 2, 0, 0, 0, 4, 107, 101, 121, 50,
    0, 0, 0, 6, 118, 97, 108, 117, 101, 50,
    0, 0, 0, 4, 107, 101, 121, 49, 0, 0,
    0, 6, 118, 97, 108, 117, 101, 49, 0, 0,
    0, 2, 0, 0, 0, 5, 115, 107, 101, 121,
    49, 0, 0, 0, 7, 115, 118, 97, 108, 117,
    101, 49, 0, 0, 0, 5, 115, 107, 101, 121,
    50, 0, 0, 0, 7, 115, 118, 97, 108, 117,
    101, 50, 0, 0, 0, 0, 0, 0, 0, 1,
    0, 0, 0, 0, 0, 0, 1, 39, -121, -85,
    30, -104, 0, 0, 0, 1, 0, 0, 0, 5,
    114, 107, 101, 121, 49, 0, 0, 0, 7, 114,
    118, 97, 108, 117, 101, 49, 1, 126, 9, -127,
    -41, -102, -13, -115)

  val serializedResourceVersion1:Array[Byte] = Array(
    0, 0, 0, 1, 0, 0, 0, 0, 41, 48,
    101, 54, 51, 49, 53, 101, 97, 45, 99, 50,
    102, 100, 45, 52, 98, 101, 102, 45, 57, 51,
    54, 101, 45, 53, 57, 99, 101, 102, 55, 57,
    52, 51, 56, 52, 49, 45, 54, 97, 52, 55,
    0, 0, 0, 0, 0, 0, 39, 16, 0, 0,
    0, 2, 0, 0, 0, 4, 107, 101, 121, 50,
    0, 0, 0, 6, 118, 97, 108, 117, 101, 50,
    0, 0, 0, 4, 107, 101, 121, 49, 0, 0,
    0, 6, 118, 97, 108, 117, 101, 49, 0, 0,
    0, 2, 0, 0, 0, 5, 115, 107, 101, 121,
    49, 0, 0, 0, 7, 115, 118, 97, 108, 117,
    101, 49, 0, 0, 0, 5, 115, 107, 101, 121,
    50, 0, 0, 0, 7, 115, 118, 97, 108, 117,
    101, 50, 0, 0, 0, 0, 0, 0, 0, 1,
    0, 0, 0, 0, 0, 0, 1, 39, -121, -74,
    107, -12, 0, 0, 0, 1, 0, 0, 0, 5,
    114, 107, 101, 121, 49, 0, 0, 0, 7, 114,
    118, 97, 108, 117, 101, 49, -59, -92, -42, -45,
    55, -57, -14, 77, 27, 12, -57, -107, -50, -33,
    79, 71
    )


  /*def testResourceSerialize = {
    val resource = new Resource

    resource.address = "0e6315ea-c2fd-4bef-936e-59cef7943841-6a47"
    resource.createDate = new Date(10000)

    resource.metadata.put("key1", "value1")
    resource.metadata.put("key2", "value2")

    resource.systemMetadata.put("skey1", "svalue1")
    resource.systemMetadata.put("skey2", "svalue2")

    resource.isVersioned = false

    val version = new ResourceVersion
    version.data = DataWrapper.wrap("my data here")
    version.systemMetadata.put("rkey1", "rvalue1")

    resource.addVersion(version)

    val bytes = resource.toByteArray

    var i=0

    for(bt <- bytes){
      if(i == 10){
        i = 0
        println()
      }
      i = i+ 1
      print(bt)
      print(", ")
    }

  }*/

  def testDeserializeVersion0 = {
    val resource = Resource.fromByteArray(serializedResourceVersion0)

    assertEquals("0e6315ea-c2fd-4bef-936e-59cef7943841-6a47", resource.address)
    assertEquals("value1", resource.metadata.get("key1").get)
    assertEquals("value2", resource.metadata.get("key2").get)
    assertEquals("svalue1", resource.systemMetadata.get("skey1").get)
    assertEquals("svalue2", resource.systemMetadata.get("skey2").get)
    assertEquals(10000l, resource.createDate.getTime)
    assertEquals(false, resource.isVersioned)
    assertEquals(1, resource.versions.size)
    assertEquals("rvalue1", resource.versions(0).systemMetadata.get("rkey1").get)

  }

  def testDeserializeVersion1 = {
    val resource = Resource.fromByteArray(serializedResourceVersion1)

    assertEquals("0e6315ea-c2fd-4bef-936e-59cef7943841-6a47", resource.address)
    assertEquals("value1", resource.metadata.get("key1").get)
    assertEquals("value2", resource.metadata.get("key2").get)
    assertEquals("svalue1", resource.systemMetadata.get("skey1").get)
    assertEquals("svalue2", resource.systemMetadata.get("skey2").get)
    assertEquals(10000l, resource.createDate.getTime)
    assertEquals(false, resource.isVersioned)
    assertEquals(1, resource.versions.size)
    assertEquals("rvalue1", resource.versions(0).systemMetadata.get("rkey1").get)
    
  }


}