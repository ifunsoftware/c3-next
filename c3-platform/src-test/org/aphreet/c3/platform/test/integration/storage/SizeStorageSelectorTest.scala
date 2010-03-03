package org.aphreet.c3.platform.test.integration.storage

import org.aphreet.c3.platform.storage.dispatcher.selector.size._
import org.aphreet.c3.platform.config.PlatformConfigManager

import org.aphreet.c3.platform.test.integration.AbstractTestWithFileSystem

import junit.framework.Assert._

class SizeStorageSelectorTest extends AbstractTestWithFileSystem{

  def testConfigPersistence = {
    
    val configAccessor = new SizeSelectorConfigAccessor
    
    val config = Map[Long, (String, Boolean)](
    	0l -> ("PureBDBStorage", true),
    	250000l -> ("FileBDBStorage", false)
    )
    
    val configManager = new PlatformConfigManager
    configManager.configDir = testDir
    configAccessor.setConfigManager(configManager)
    
    configAccessor store config
    
    val selector = new SizeStorageSelector
    selector.setSizeSelectorConfigAccessor(configAccessor)
    selector.init
    
    val entries = selector.configEntries
    
    println(entries)
    
    assertEquals(("PureBDBStorage", true), selector.storageTypeForSize(1024l))
    assertEquals(("FileBDBStorage", false), selector.storageTypeForSize(500000l))
    
  }

}
