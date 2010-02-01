package org.aphreet.c3.platform.test.integration.config

import scala.collection.mutable.HashMap

import org.aphreet.c3.platform.storage.dispatcher.selector.mime.{MimeTypeConfigAccessor, MimeConfigEntry}

import junit.framework.Assert._

class MimeTypeConfigTest extends AbstractTestWithFileSystem{

  def testConfigPersistence = {
    
    val configAccessor = new MimeTypeConfigAccessor
    
    val config = Map(
    	"*/*" -> ("PureBDBStorage", false),
    	"image/*" -> ("FileBDBStorage", true),
    	"image/png" -> ("PureBDBStorage", true)
    )
     
    configAccessor.storeConfig(config, testDir)
    
    val readConfig = configAccessor.loadConfig(testDir)
    
    assertEquals(config, readConfig)
    
  }
}
