/*
 * Copyright (c) 2013, Mikhail Malygin
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
 * 3. Neither the name of the iFunSoftware nor the names of its contributors
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

package org.aphreet.c3.platform.remote.test.replication

import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.remote.replication.impl.data.stats.DelayHistory

class DelayHistoryTestCase extends TestCase{

  def testHistory{
    val history = new DelayHistory

    history.add(500, 1000)
    history.add(300, 1000) //300
    history.add(100, 1000)

    history.add(300, 2000) //400
    history.add(500, 2000)

    history.add(100, 3000) //100

    assertEquals(100, history.averageDelay(1, 3000))
    assertEquals(250, history.averageDelay(2, 3000))
    assertEquals(266, history.averageDelay(3, 3000))

    assertEquals(0, history.averageDelay(1, 4000))
    assertEquals(100, history.averageDelay(2, 4000))
    assertEquals(250, history.averageDelay(3, 4000))
  }

}
