/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.aphreet.c3.platform.test.unit

import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.resource.{DataWrapper, ResourceVersion, Resource}
import java.util.Date


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


  def testJSON = {
    val resource = new Resource

    resource.address = "0e6315ea-c2fd-4bef-936e-59cef7943841-6a47"
    resource.createDate = new Date(1273087676152l)

    resource.metadata.put("ke\"1", "value1")
    resource.metadata.put("key2", "value2")

    resource.systemMetadata.put("skey1", "svalue1")
    resource.systemMetadata.put("skey2", "svalue2")

    resource.isVersioned = true

    val version = new ResourceVersion
    version.data = DataWrapper.wrap("my data here")
    version.systemMetadata.put("rkey1", "rvalue1")
    version.date = new Date(1273087676152l)

    resource.addVersion(version)

    val version2 = new ResourceVersion
    version2.data = DataWrapper.wrap("my data here")
    version2.systemMetadata.put("rkey331", "rvalu3e1")
    version2.date = new Date(1273087676195l)

    resource.addVersion(version2)

    val json = resource.toJSON(false)

    val expected =
"""{
	"address": "0e6315ea-c2fd-4bef-936e-59cef7943841-6a47",
	"createDate": 1273087676152,
	"metadata": {
		"key2": "value2",
		"ke\"1": "value1"
	},
	"versions": [
		{
			"createDate": 1273087676152,
			"dataLength": 12
		},
		{
			"createDate": 1273087676195,
			"dataLength": 12
		}
	]
}"""
    assertEquals(expected, json)
  }

  def testJSONFull = {
    val resource = new Resource

    resource.address = "0e6315ea-c2fd-4bef-936e-59cef7943841-6a47"
    resource.createDate = new Date(1273087676152l)

    resource.metadata.put("ke\"1", "value1")
    resource.metadata.put("key2", "value2")

    resource.systemMetadata.put("skey1", "svalue1")
    resource.systemMetadata.put("skey2", "svalue2")

    resource.isVersioned = true

    val version = new ResourceVersion
    version.data = DataWrapper.wrap("my data here")
    version.systemMetadata.put("rkey1", "rvalue1")
    version.date = new Date(1273087676152l)

    resource.addVersion(version)

    val version2 = new ResourceVersion
    version2.data = DataWrapper.wrap("my data here")
    version2.systemMetadata.put("rkey331", "rvalu3e1")
    version2.date = new Date(1273087676195l)

    resource.addVersion(version2)

    val json = resource.toJSON(true)

    val expected =
"""{
	"address": "0e6315ea-c2fd-4bef-936e-59cef7943841-6a47",
	"createDate": 1273087676152,
	"metadata": {
		"key2": "value2",
		"ke\"1": "value1"
	},
	"systemMetadata": {
		"skey1": "svalue1",
		"skey2": "svalue2"
	},
	"versions": [
		{
			"createDate": 1273087676152,
			"dataLength": 12,
			"revision": 0,
			"systemMetadata": {
				"rkey1": "rvalue1"
			}
		},
		{
			"createDate": 1273087676195,
			"dataLength": 12,
			"revision": 0,
			"systemMetadata": {
				"rkey331": "rvalu3e1"
			}
		}
	]
}"""
    assertEquals(expected, json)
  }


}