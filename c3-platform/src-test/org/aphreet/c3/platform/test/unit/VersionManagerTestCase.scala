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

import org.easymock.EasyMock._
import org.osgi.framework.{BundleContext, Bundle}
import org.aphreet.c3.platform.config.impl.VersionManagerImpl
import java.util.Hashtable
import org.scalatest.mock.EasyMockSugar

class VersionManagerTestCase extends TestCase with EasyMockSugar{

  def testVersions = {
    val manager = new VersionManagerImpl

    manager.setBundleContext(createBundleContext)

    val modules = manager.listC3Modules

    assertEquals(4, modules.size)
    assertEquals("1.0.100", modules.get("org.aphreet.c3.platform").get)
    assertEquals("1.0.101", modules.get("org.aphreet.c3.storage").get)
    assertEquals("1.0.102", modules.get("org.aphreet.c3.remote-api").get)
    assertEquals("1.0.103", modules.get("org.aphreet.c3.search").get)
    
  }

  def createBundleContext:BundleContext = {


    val bundles:Array[Bundle] = Array(
      createBundle("org.aphreet.c3.platform", "1.0.100"),
      createBundle("org.aphreet.c3.storage", "1.0.101"),
      createBundle("org.aphreet.c3.remote-api", "1.0.102"),
      createBundle("org.aphreet.c3.search", "1.0.103"),
      createBundle("org.aphreet.some.other.bundle", "1.0.104")
    );

    val context = mock[BundleContext]

    call(context.getBundles).andStubReturn(bundles)

    replay(context)

    context
  }

  def createBundle(name:String, version:String):Bundle = {

    val headerDictionary = new Hashtable[String,String]
    headerDictionary.put("Bundle-Version", version)

    val bundle = mock[Bundle]

    call(bundle.getSymbolicName).andStubReturn(name)

    call(bundle.getHeaders).andStubReturn(headerDictionary)

    replay(bundle)
    
    bundle
  }


  
}