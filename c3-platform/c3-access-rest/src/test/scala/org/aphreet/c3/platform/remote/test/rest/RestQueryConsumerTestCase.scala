package org.aphreet.c3.platform.remote.test.rest

import junit.framework.TestCase
import junit.framework.Assert._
import org.easymock.EasyMock._
import org.aphreet.c3.platform.remote.rest.query.RestQueryConsumer
import java.io.{StringWriter, PrintWriter}
import org.aphreet.c3.platform.remote.rest.response.ResultWriter
import org.aphreet.c3.platform.resource.Resource

class RestQueryConsumerTestCase extends TestCase{

  def testConsumeLimits(){

    val writer = new StringWriter()
    val pw = new PrintWriter(writer)

    val resources = Array(new Resource, new Resource, new Resource, new Resource)

    val resultWriter = createMock(classOf[ResultWriter])

    resources.foreach(expect(resultWriter.writeResponse()))

    replay(resultWriter)

    val consumer = new RestQueryConsumer(pw, resultWriter, 0, Some(3))

    assertTrue(consumer.consume(new Resource))
    assertTrue(consumer.consume(new Resource))
    assertTrue(consumer.consume(new Resource))
    assertFalse(consumer.consume(new Resource))

    verify(resultWriter)
  }

}
