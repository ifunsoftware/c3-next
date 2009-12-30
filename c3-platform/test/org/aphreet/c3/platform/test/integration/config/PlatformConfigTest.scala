package org.aphreet.c3.platform.test.integration.config

import org.aphreet.c3.platform.config.accessor.PlatformConfigAccessor
import scala.collection.jcl.HashMap

import junit.framework.Assert._

class PlatformConfigTest extends AbstractTestWithFileSystem{

  def testConfigPersistence = {

    val configAccessor = new PlatformConfigAccessor
    
    val config = new HashMap[String, String]
    config.put("prop1", "val1")
    config.put("prop2", "val2")
    config.put("prop3", "val3")
    config.put("prop4", "val4")
    
    configAccessor.storeConfig(config, testDir)
    
    val readConfig = configAccessor.loadConfig(testDir)
    
    assertEquals(config, readConfig)
  }
  
}
