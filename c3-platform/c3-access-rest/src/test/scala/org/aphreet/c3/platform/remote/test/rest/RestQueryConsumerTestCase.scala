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

package org.aphreet.c3.platform.remote.test.rest

import junit.framework.TestCase
import junit.framework.Assert._
import org.easymock.EasyMock._
import org.aphreet.c3.platform.remote.rest.query.RestQueryConsumer
import java.io.{Writer, StringWriter, PrintWriter}
import org.aphreet.c3.platform.remote.rest.response.ResultWriter
import org.aphreet.c3.platform.resource.Resource
import javax.servlet.http.HttpServletResponse

class RestQueryConsumerTestCase extends TestCase{

  def testConsumeLimits(){

    val writeDelegate = createMock(classOf[WriteDelegate])

    expect(writeDelegate.println(""))
    expect(writeDelegate.println("1"))
    expect(writeDelegate.println("2"))
    expect(writeDelegate.println("3"))
    expect(writeDelegate.println(""))

    val resultWriter = createMock(classOf[ResultWriter])

    replay(resultWriter, writeDelegate)

    val consumer = new RestQueryConsumer(new WriterMock(writeDelegate), resultWriter, 0, Some(3))

    assertTrue(consumer.consume(resource("1")))
    assertTrue(consumer.consume(resource("2")))
    assertFalse(consumer.consume(resource("3")))

    consumer.close()

    verify(resultWriter, writeDelegate)
  }

  def testConsume(){

    val writeDelegate = createMock(classOf[WriteDelegate])

    expect(writeDelegate.println(""))
    expect(writeDelegate.println("1"))
    expect(writeDelegate.println("2"))
    expect(writeDelegate.println("3"))
    expect(writeDelegate.println(""))

    val resultWriter = createMock(classOf[ResultWriter])

    replay(resultWriter, writeDelegate)

    val consumer = new RestQueryConsumer(new WriterMock(writeDelegate), resultWriter, 0, None)

    assertTrue(consumer.consume(resource("1")))
    assertTrue(consumer.consume(resource("2")))
    assertTrue(consumer.consume(resource("3")))

    consumer.close()

    verify(resultWriter, writeDelegate)
  }

  def testConsumeWithSkip(){

    val writeDelegate = createMock(classOf[WriteDelegate])

    expect(writeDelegate.println(""))
    expect(writeDelegate.println("4"))
    expect(writeDelegate.println("5"))
    expect(writeDelegate.println(""))

    val resultWriter = createMock(classOf[ResultWriter])

    replay(resultWriter, writeDelegate)

    val consumer = new RestQueryConsumer(new WriterMock(writeDelegate), resultWriter, 3, Some(2))

    assertTrue(consumer.consume(resource("1")))
    assertTrue(consumer.consume(resource("2")))
    assertTrue(consumer.consume(resource("3")))
    assertTrue(consumer.consume(resource("4")))
    assertFalse(consumer.consume(resource("5")))

    consumer.close()

    verify(resultWriter, writeDelegate)
  }

  trait WriteDelegate{

    def println(line: String)

  }

  class WriterMock(delegate: WriteDelegate) extends PrintWriter(new StringWriter()){

    override def println(line: String){
      delegate.println(line)
    }
  }

  def resource(address: String): Resource = {
    val resource = new Resource
    resource.address = address
    resource
  }
}
