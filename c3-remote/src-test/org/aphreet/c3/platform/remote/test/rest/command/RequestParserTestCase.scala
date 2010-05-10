package org.aphreet.c3.platform.remote.test.rest.command

import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.remote.rest._

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: May 5, 2010
 * Time: 12:27:59 AM
 * To change this template use File | Settings | File Templates.
 */

class RequestParserTestCase extends TestCase {

  def testWrongUri{

    checkFailedParams("/c3-remote/", "/c3-remote")
    checkFailedParams("/c3-remote/", "/c3-remote/")
    checkFailedParams("/c3-remote/resource", "/c3-remote")
    checkFailedParams("/c3-remote/resource/", "/c3-remote")
    checkFailedParams("/c3-remote/search", "/c3-remote")
    checkFailedParams("/c3-remote/search/", "/c3-remote")

    checkFailedParams("/", "/")
    checkFailedParams("/resource", "/")
    checkFailedParams("/resource/", "/")
    checkFailedParams("/search", "/")
    checkFailedParams("/search/", "/")
    
  }

  def testEmptyUri{
    checkOkParams("/c3-remote/resource", "/c3-remote")
    checkOkParams("/c3-remote/resource/", "/c3-remote")
    checkOkParams("/c3-remote/search", "/c3-remote")
    checkOkParams("/c3-remote/search/", "/c3-remote")

    checkOkParams("/resource", "/")
    checkOkParams("/resource/", "/")
    checkOkParams("/search", "/")
    checkOkParams("/search/", "/")
  }

  def testUri1{

    val command = new Command("/c3-remote/resource/0000-0000-0000-0000", "/c3-remote")


    assertEquals(ResourceRequest, command.requestType)
    assertEquals("0000-0000-0000-0000", command.query)
    assertEquals(ResourceMetadata, command.resourcePart)
    assertEquals(-1, command.version)
  }

  def testUri2{

    val command = new Command("/c3-remote/resource/0000-0000-0000-0000/data", "/c3-remote")


    assertEquals(ResourceRequest, command.requestType)
    assertEquals("0000-0000-0000-0000", command.query)
    assertEquals(ResourceData, command.resourcePart)
    assertEquals(-1, command.version)
  }

  def testUri3{
    val command = new Command("/c3-remote/resource/0000-0000-0000-0000/metadata", "/c3-remote")


    assertEquals(ResourceRequest, command.requestType)
    assertEquals("0000-0000-0000-0000", command.query)
    assertEquals(ResourceMetadata, command.resourcePart)
    assertEquals(-1, command.version)
  }

  def testUri4{
     val command = new Command("/c3-remote/resource/0000-0000-0000-0000/data/23", "/c3-remote")


    assertEquals(ResourceRequest, command.requestType)
    assertEquals("0000-0000-0000-0000", command.query)
    assertEquals(ResourceData, command.resourcePart)
    assertEquals(23, command.version)
  }

  def testUri5{
     val command = new Command("/c3-remote/search/some_request_here", "/c3-remote")


    assertEquals(SearchRequest, command.requestType)
    assertEquals("some_request_here", command.query)
  }


  def checkFailedParams(uri:String, context:String) = {
     try{
      val command = new Command(uri, context)
      assertFalse(false)
    }catch{
      case e:URIParseException => assertTrue(true)
      case e => assertFalse(false)
    }
  }

  def checkOkParams(uri:String, context:String) = {
     try{
      val command = new Command(uri, context)
      assertTrue(true)
    }catch{
      case e => assertFalse(false)
    }
  }

}