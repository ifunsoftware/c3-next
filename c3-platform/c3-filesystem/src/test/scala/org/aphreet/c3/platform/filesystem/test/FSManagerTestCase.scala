/**
 * Copyright (c) 2011, Mikhail Malygin
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

package org.aphreet.c3.platform.filesystem.test

import junit.framework.TestCase
import org.aphreet.c3.platform.access.AccessManager
import org.aphreet.c3.platform.filesystem.impl.FSManagerImpl
import junit.framework.Assert._
import org.easymock.EasyMock._
import org.aphreet.c3.platform.filesystem._
import org.aphreet.c3.platform.resource.{DataStream, ResourceVersion, Resource}

class FSManagerTestCase extends TestCase with FSTestHelpers{

  def testDeletePermission() {

    val fsManager = new FSManagerImpl

    fsManager.fsRoots = Map("domain id " -> "address", "domain id 2" -> "address2")

    val resource = new Resource

    assertTrue(fsManager.resourceCanBeDeleted(resource))


    resource.systemMetadata.put(Node.NODE_FIELD_TYPE, Node.NODE_TYPE_FILE)

    assertTrue(fsManager.resourceCanBeDeleted(resource))


    val directory = Directory.emptyDirectory("domain id", "name")

    directory.resource.address = "some other address"

    //empty non-root directory
    assertTrue(fsManager.resourceCanBeDeleted(directory.resource))

    directory.resource.address = "address"

    //empty root directory
    assertFalse(fsManager.resourceCanBeDeleted(directory.resource))

    directory.addChild("name", "address", leaf = true)

    //non-empty root directory
    assertFalse(fsManager.resourceCanBeDeleted(directory.resource))

    directory.resource.address = "some other address"

    //non-empty non-root directory
    assertTrue(fsManager.resourceCanBeDeleted(directory.resource))

    directory.removeChild("name")


    //Just a check that we still have a deleted child entry
    //in the child list
    assertTrue(directory.allChildren.length > 0)

    assertTrue(fsManager.resourceCanBeDeleted(directory.resource))

    val file = File.createFile(new Resource, "some domain", "name")

    assertTrue(fsManager.resourceCanBeDeleted(file.resource))
  }

  def testUpdatePermission() {

    val fsManager = new FSManagerImpl

    val resource = new Resource

    assertTrue(fsManager.resourceCanBeUpdated(resource))

    resource.systemMetadata.put(Node.NODE_FIELD_TYPE, Node.NODE_TYPE_FILE)

    assertTrue(fsManager.resourceCanBeUpdated(resource))

    val directory = Directory.emptyDirectory("domain id", "name")

    //empty directory
    assertTrue(fsManager.resourceCanBeUpdated(directory.resource))

    directory.addChild("name", "address", leaf = true)

    //non-empty directory
    assertTrue(fsManager.resourceCanBeUpdated(directory.resource))

    //some non-directory data

    val serializedData:Array[Byte] = Array(
      0, 0, 23, 41, 48,
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

    val resource0 = new Resource

    val version = new ResourceVersion
    version.data = DataStream.create(serializedData)

    resource0.addVersion(version)

    assertTrue(fsManager.resourceCanBeUpdated(resource0))

    resource0.systemMetadata.put(Node.NODE_FIELD_TYPE, Node.NODE_TYPE_DIR)

    assertFalse(fsManager.resourceCanBeUpdated(resource0))

  }

  def testAddressToPathConversion() {

    val accessManager = createMock(classOf[AccessManager])
    expect(accessManager.get("00000000-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("file0", "00000001-c2fd-4bef-936e-59cef7943840-6a47")).anyTimes
    expect(accessManager.get("00000001-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("third_level_dir", "00000002-c2fd-4bef-936e-59cef7943840-6a47")).anyTimes
    expect(accessManager.get("00000002-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("second_level_dir", "00000003-c2fd-4bef-936e-59cef7943840-6a47")).anyTimes
    expect(accessManager.get("00000003-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("", null)).anyTimes
    replay(accessManager)

    val fsManager = new FSManagerImpl

    fsManager.accessManager = accessManager

    assertEquals(Some("/second_level_dir/third_level_dir/file0"), fsManager.lookupResourcePath("00000000-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals(Some("/second_level_dir/third_level_dir"), fsManager.lookupResourcePath("00000001-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals(Some("/second_level_dir"), fsManager.lookupResourcePath("00000002-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals(None, fsManager.lookupResourcePath("00000003-c2fd-4bef-936e-59cef7943840-6a47"))


    verify(accessManager)
  }

  def testNonFileAddressToPathConversion() {

    val accessManager = createMock(classOf[AccessManager])
    expect(accessManager.get("00000000-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("file0", "00000001-c2fd-4bef-936e-59cef7943840-6a47")).anyTimes
    expect(accessManager.get("00000001-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub(null, "00000002-c2fd-4bef-936e-59cef7943840-6a47")).anyTimes
    expect(accessManager.get("00000002-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub(null, null)).anyTimes
    expect(accessManager.get("00000003-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("", null)).anyTimes
    replay(accessManager)

    val fsManager = new FSManagerImpl

    fsManager.accessManager = accessManager

    assertEquals(None, fsManager.lookupResourcePath("00000000-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals(None, fsManager.lookupResourcePath("00000001-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals(None, fsManager.lookupResourcePath("00000002-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals(None, fsManager.lookupResourcePath("00000003-c2fd-4bef-936e-59cef7943840-6a47"))

    verify(accessManager)
  }

  def testSplitPath() {

    val manager = new FSManagerImpl

    val result = manager.splitPath("test/dir/file.jpeg")

    assertEquals(result._1, "test/dir")
    assertEquals(result._2, "file.jpeg")
  }
}