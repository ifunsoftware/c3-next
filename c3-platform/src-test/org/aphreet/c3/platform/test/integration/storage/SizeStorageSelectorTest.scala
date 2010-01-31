package org.aphreet.c3.platform.test.integration.storage

import org.aphreet.c3.platform.storage.dispatcher.selector.size._
import org.aphreet.c3.platform.config.PlatformConfigManager

import junit.framework.Assert._

class SizeStorageSelectorTest extends AbstractTestWithFileSystem{

  def testConfigPersistence = {
    
    val configAccessor = new SizeSelectorConfigAccessor
    
    val config = Map[Long, String](
    	0l -> "PureBDBStorage",
    	250000l -> "FileBDBStorage"
    )
    
    val configManager = new PlatformConfigManager
    configManager.configDir = testDir
    configAccessor.setConfigManager(configManager)
    
    configAccessor store config
    
    val selector = new SizeStorageSelector
    selector.setSizeSelectorConfigAccessor(configAccessor)
    selector.init
    
    assertEquals("PureBDBStorage", selector.storageTypeForSize(1024l))
    assertEquals("FileBDBStorage", selector.storageTypeForSize(500000l))
    
    selector.addEntry(())
    
    
  }

}
