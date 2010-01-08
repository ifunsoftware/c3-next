package org.aphreet.c3.platform.test.integration

import junit.framework.TestCase

import java.io.File

class AbstractTestWithFileSystem extends TestCase{

  var testDir:File = null
  
  override def setUp{
	testDir = new File(System.getProperty("user.home"), "c3_int_test")
    testDir.mkdirs
  }
  
  override def tearDown{
    def delDir(directory:File) {
      if(directory.isDirectory) directory.listFiles.foreach(delDir(_))
      directory.delete
    }
    delDir(testDir)
  }
  
}
