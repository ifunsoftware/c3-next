package org.aphreet.c3.platform.test.integration.storage

import eu.medsea.mimeutil.MimeType

import org.aphreet.c3.platform.storage.dispatcher.selector.mime._
import org.aphreet.c3.platform.config.PlatformConfigManager

import junit.framework.Assert._

class MimeTypeStorageSelectorTest extends AbstractTestWithFileSystem{

  def testConfigPersistence = {
    
    val configAccessor = new MimeTypeConfigAccessor
    
    val config = Map(
    	"*/*" -> ("PureBDBStorage", false),
    	"image/*" -> ("FileBDBStorage", true),
    	"image/png" -> ("PureBDBStorage", true)
    )
     
    configAccessor.storeConfig(config, testDir)
   
    val configManager = new PlatformConfigManager
    configManager.configDir = testDir
    
    configAccessor.setConfigManager(configManager)
    
    val selector = new MimeTypeStorageSelector
    selector.setConfigAccessor(configAccessor)
    selector.init
    
    assertEquals(("PureBDBStorage", true),selector.storageTypeForMimeType(new MimeType("image/png")))
    assertEquals(("FileBDBStorage", true),selector.storageTypeForMimeType(new MimeType("image/jpeg")))
    assertEquals(("PureBDBStorage", false),selector.storageTypeForMimeType(new MimeType("application/pdf")))
    
  }
}
