package org.aphreet.c3.platform.test.integration.config

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.config.PlatformConfigManager
import org.aphreet.c3.platform.storage.impl.StorageConfigAccessorImpl
import org.aphreet.c3.platform.storage._

import org.aphreet.c3.platform.test.integration.AbstractTestWithFileSystem

import junit.framework.Assert._
class StorageConfigTest extends AbstractTestWithFileSystem{


  def testConfigPersistence = {
    
    val config  = List(
      StorageParams("11", List(), new Path("C:\\data\\file\\"), "PureBDBStorage", RW("migration")),
      StorageParams("22", List("33","44"), new Path("C:\\data\\file1\\"), "FileBDBStorage", RO(""))
    )
    
    val configManager = new PlatformConfigManager
    configManager.configDir = testDir
    
    val accessor = new StorageConfigAccessorImpl
    accessor.setConfigManager(configManager)
    
    
    accessor.storeConfig(config, testDir)
    
    val readConfig = accessor.loadConfig(testDir)
    
    assertEquals(config, readConfig)
    
    val newParams = StorageParams("22", List("33","44"), new Path("C:\\data\\file1\\"), "FileBDBStorage", RW(""))
    
    
    accessor.update(config => newParams :: config.filter(_.id != newParams.id))
    
    assertEquals(newParams, accessor.load.head) 
    
  }
}
