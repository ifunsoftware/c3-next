package org.aphreet.c3.platform.remote.test.rest.command

import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.remote.rest.{ResourceData, ResourceMetadata, Command}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: May 5, 2010
 * Time: 12:27:59 AM
 * To change this template use File | Settings | File Templates.
 */

class RequestParserTestCase extends TestCase {

  def testSimpleUri{

    val result = Command.parseURI("/c3-remote/")

    assertEquals(null, result._1)
    assertEquals(ResourceMetadata, result._2)
    assertEquals(-1, result._3)
  }

  def testUri1{
    val result = Command.parseURI("/c3-remote/0000-0000-0000-0000")

    assertEquals("0000-0000-0000-0000", result._1)
    assertEquals(ResourceMetadata, result._2)
    assertEquals(-1, result._3)
  }

  def testUri2{
    val result = Command.parseURI("/c3-remote/0000-0000-0000-0000/data")

    assertEquals("0000-0000-0000-0000", result._1)
    assertEquals(ResourceData, result._2)
    assertEquals(-1, result._3)
  }

  def testUri3{
    val result = Command.parseURI("/c3-remote/0000-0000-0000-0000/metadata")

    assertEquals("0000-0000-0000-0000", result._1)
    assertEquals(ResourceMetadata, result._2)
    assertEquals(-1, result._3)
  }

  def testUri4{
    val result = Command.parseURI("/c3-remote/0000-0000-0000-0000/data/23")

    assertEquals("0000-0000-0000-0000", result._1)
    assertEquals(ResourceData, result._2)
    assertEquals(23, result._3)
  }

}