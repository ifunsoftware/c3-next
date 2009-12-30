package org.aphreet.c3.platform.test.integration

import junit.framework.TestCase
import java.io.File

abstract class AbstractTestCase extends TestCase{

  protected def getTestDirectory:File = {
    val dir = new File(System.getProperty("user.home"), "c3_int_test")
    
    dir.mkdirs
    
    dir
  }
  
}
