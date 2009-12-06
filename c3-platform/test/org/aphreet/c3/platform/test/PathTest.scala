package org.aphreet.c3.platform.test

import java.io.File

import junit.framework.{TestCase, Assert}

import org.aphreet.c3.platform.common._

class PathTest extends TestCase{

  def testUnixPaths{
    assertEquals(path("/usr/bin/test").toString, "/usr/bin/test")
	assertEquals(path("/uSr/bin/teSt").toString, "/uSr/bin/teSt")
	assertEquals(path("/usr/bin/test/").toString, "/usr/bin/test")
  }
  
  def testWinPaths{
    assertEquals(path("D:\\my\\path").toString, "D:/my/path")
    assertEquals(path("d:\\my\\path").toString, "D:/my/path")
    assertEquals(path("d:/my/path").toString, "D:/my/path")
    assertEquals(path("D:/my/path").toString, "D:/my/path")
    assertEquals(path("D:/my/path/").toString, "D:/my/path")
    assertEquals(path("D:\\my\\path\\").toString, "D:/my/path")
  }
  
  private def path(string:String):Path = new Path(string)
  
  private def path(file:File):Path = new Path(file);

  private def assertEquals(value:Any, value2:Any){
	Assert.assertEquals(value, value2)
  }
}
