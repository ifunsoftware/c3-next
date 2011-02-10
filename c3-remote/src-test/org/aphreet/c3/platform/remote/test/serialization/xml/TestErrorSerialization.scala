package org.aphreet.c3.platform.remote.test.serialization.xml

import junit.framework.TestCase
import org.aphreet.c3.platform.remote.rest.response.Error
import org.aphreet.c3.platform.remote.rest.serialization.XStreamFactory

/**
 * Created by IntelliJ IDEA.
 * User: malygm
 * Date: 2/10/11
 * Time: 4:06 PM
 * To change this template use File | Settings | File Templates.
 */

class TestErrorSerialization extends TestCase{

  def testSerialization{
    val xStream = new XStreamFactory().createXMLStream

    println(xStream.toXML(new Error("error message here")))

  }

  def testJsonSerialization{
    val xStream = new XStreamFactory().createJSONStream

    println(xStream.toXML(new Error("error message here")))
  }
}