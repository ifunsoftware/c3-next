package org.aphreet.c3.platform.search

import java.io.File
import org.aphreet.c3.platform.search.lucene.impl.{Field, FieldConfiguration, SearchConfigurationAccessor}
import org.aphreet.c3.platform.test.integration.AbstractTestWithFileSystem
import junit.framework.Assert._

class SearchConfigurationAccessorTestCase extends AbstractTestWithFileSystem{

  def testConfigPersistence() {

    val configAccessor = new SearchConfigurationAccessor

    val config = FieldConfiguration(List(Field("f2", 2.0f, 2), Field("f1", 1, 1)))

    val fileName = "config.json"

    configAccessor.storeConfig(config, new File(testDir, fileName))

    val readConfig = configAccessor.loadConfig(new File(testDir, fileName))

    assertEquals(config, readConfig)
  }
}
