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

import org.aphreet.c3.platform.common.Path

import java.io.File
import junit.framework.Assert
import junit.framework.TestCase


class PathTest extends TestCase {
  def testUnixPaths {
    assertEquals(path("/usr/bin/test").toString, "/usr/bin/test")
    assertEquals(path("/uSr/bin/teSt").toString, "/uSr/bin/teSt")
    assertEquals(path("/usr/bin/test/").toString, "/usr/bin/test")
    assertEquals(true, true)
  }

  def testWinPaths {
    assertEquals(path("D:\\my\\path").toString, "D:/my/path")
    assertEquals(path("d:\\my\\path").toString, "D:/my/path")
    assertEquals(path("d:/my/path").toString, "D:/my/path")
    assertEquals(path("D:/my/path").toString, "D:/my/path")
    assertEquals(path("D:/my/path/").toString, "D:/my/path")
    assertEquals(path("D:\\my\\path\\").toString, "D:/my/path")
  }

  private def path(string: String): Path = new Path(string)

  private def path(file: File): Path = new Path(file);

  private def assertEquals(value: Any, value2: Any) {
    Assert.assertEquals(value, value2)
  }
}
