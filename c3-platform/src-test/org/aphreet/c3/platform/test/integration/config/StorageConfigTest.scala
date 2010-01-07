package org.aphreet.c3.platform.test.integration.config

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.config.accessor.StorageConfigAccessor
import org.aphreet.c3.platform.storage._

import junit.framework.Assert._

class StorageConfigTest extends AbstractTestWithFileSystem{

  def testConfigPersistence = {
    
    val config  = List(
      StorageParams("11", List(), new Path("C:\\data\\file\\"), "PureBDBStorage", RW("migration")),
      StorageParams("22", List("33","44"), new Path("C:\\data\\file1\\"), "FileBDBStorage", RO(""))
    )
    
    
    
    val accessor = new StorageConfigAccessor
    
    accessor.storeConfig(config, testDir)
    
    val readConfig = accessor.loadConfig(testDir)
    
    assertEquals(config, readConfig)
  }
}
