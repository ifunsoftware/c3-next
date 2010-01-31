package org.aphreet.c3.platform.test.integration.config

import junit.framework.Assert._

import org.aphreet.c3.platform.storage.dispatcher.selector.size.SizeSelectorConfigAccessor

class SizeSelectorConfigTest extends AbstractTestWithFileSystem{

  def testConfigPersistence {
    
    val configAccessor = new SizeSelectorConfigAccessor
    
    val config = Map[Long, String](
    	0l -> "PureBDBStorage",
    	250000l -> "FileBDBStorage"
    )
     
    configAccessor.storeConfig(config, testDir)
    
    val readConfig = configAccessor.loadConfig(testDir)
    
    assertEquals(config, readConfig)
  }
  
}
