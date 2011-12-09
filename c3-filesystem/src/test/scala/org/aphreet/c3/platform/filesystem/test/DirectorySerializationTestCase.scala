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

import org.aphreet.c3.platform.filesystem.Directory
import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.resource.{DataStream, Resource, ResourceVersion}

class DirectorySerializationTestCase extends TestCase{

  def testIncorrectContent() {
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


    val resource = new Resource

    val version = new ResourceVersion
    version.data = DataStream.create(serializedData)

    resource.addVersion(version)

    try{
      val directory = Directory(resource)

      for(child <- directory.getChildren){
        println(child)
      }
      assertFalse(false)
    }catch{
      case e => assertTrue(true)
    }
  }

}