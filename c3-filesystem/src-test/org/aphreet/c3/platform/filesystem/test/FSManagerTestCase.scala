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
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.filesystem.Node
import junit.framework.Assert._
import org.easymock.EasyMock._

class FSManagerTestCase extends TestCase{

  def testAddressToPathConversion = {

    val accessManager = createMock(classOf[AccessManager])
    expect(accessManager.get("00000000-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("file0", "00000001-c2fd-4bef-936e-59cef7943840-6a47")).anyTimes
    expect(accessManager.get("00000001-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("third_level_dir", "00000002-c2fd-4bef-936e-59cef7943840-6a47")).anyTimes
    expect(accessManager.get("00000002-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("second_level_dir", "00000003-c2fd-4bef-936e-59cef7943840-6a47")).anyTimes
    expect(accessManager.get("00000003-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("", null)).anyTimes
    replay(accessManager)

    val fsManager = new FSManagerImpl

    fsManager.setAccessManager(accessManager)
    
    assertEquals("/second_level_dir/third_level_dir/file0", fsManager.lookupResourcePath("00000000-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals("/second_level_dir/third_level_dir", fsManager.lookupResourcePath("00000001-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals("/second_level_dir", fsManager.lookupResourcePath("00000002-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals("", fsManager.lookupResourcePath("00000003-c2fd-4bef-936e-59cef7943840-6a47"))


    verify(accessManager)
  }

  def testNonFileAddressToPathConversion = {

    val accessManager = createMock(classOf[AccessManager])
    expect(accessManager.get("00000000-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("file0", "00000001-c2fd-4bef-936e-59cef7943840-6a47")).anyTimes
    expect(accessManager.get("00000001-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub(null, "00000002-c2fd-4bef-936e-59cef7943840-6a47")).anyTimes
    expect(accessManager.get("00000002-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub(null, null)).anyTimes
    expect(accessManager.get("00000003-c2fd-4bef-936e-59cef7943840-6a47")).andReturn(resourceStub("", null)).anyTimes
    replay(accessManager)

    val fsManager = new FSManagerImpl

    fsManager.setAccessManager(accessManager)

    assertEquals("", fsManager.lookupResourcePath("00000000-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals("", fsManager.lookupResourcePath("00000001-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals("", fsManager.lookupResourcePath("00000002-c2fd-4bef-936e-59cef7943840-6a47"))
    assertEquals("", fsManager.lookupResourcePath("00000003-c2fd-4bef-936e-59cef7943840-6a47"))



    verify(accessManager)
  }

  def resourceStub(name:String, parentAddress:String):Resource = {
    val resource = new Resource

    if(name != null){
      resource.systemMetadata.put(Node.NODE_FIELD_NAME, name)
    }
    
    if(parentAddress != null){
      resource.systemMetadata.put(Node.NODE_FIELD_PARENT, parentAddress)
    }

    resource
  }
}