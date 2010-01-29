package org.aphreet.c3.platform.test.integration.storage

import eu.medsea.mimeutil.MimeType

import org.aphreet.c3.platform.storage.dispatcher.selector.mime._
import org.aphreet.c3.platform.config.PlatformConfigManager

import junit.framework.Assert._

class MimeTypeStorageSelectorTest extends AbstractTestWithFileSystem{

  def testConfigPersistence = {
    
    val configAccessor = new MimeTypeConfigAccessor
    
    val config = List(
    	MimeConfigEntry("*/*", "PureBDBStorage", false),
    	MimeConfigEntry("image/*", "FileBDBStorage", true),
    	MimeConfigEntry("image/png", "PureBDBStorage", true)
    )
     
    configAccessor.storeConfig(config, testDir)
   
    val configManager = new PlatformConfigManager
    configManager.configDir = testDir
    
    configAccessor.setConfigManager(configManager)
    
    val selector = new MimeTypeStorageSelector
    selector.setConfigAccessor(configAccessor)
    selector.init
    
    assertEquals(MimeConfigEntry("image/png", "PureBDBStorage", true),selector.storageTypeForMimeType(new MimeType("image/png")))
    assertEquals(MimeConfigEntry("image/*", "FileBDBStorage", true),selector.storageTypeForMimeType(new MimeType("image/jpeg")))
    assertEquals(MimeConfigEntry("*/*", "PureBDBStorage", false),selector.storageTypeForMimeType(new MimeType("application/pdf")))
    
  }
}
