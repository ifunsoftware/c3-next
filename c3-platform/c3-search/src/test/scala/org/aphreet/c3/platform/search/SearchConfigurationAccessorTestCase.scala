package org.aphreet.c3.platform.search

import impl.{Field, FieldConfiguration, SearchConfigurationAccessor}
import org.aphreet.c3.platform.test.integration.AbstractTestWithFileSystem
import java.io.File
import junit.framework.Assert._
import org.aphreet.c3.platform.config.impl.MemoryConfigPersister

class SearchConfigurationAccessorTestCase extends AbstractTestWithFileSystem{

  def testConfigPersistence() {

    val configAccessor = new SearchConfigurationAccessor(new MemoryConfigPersister)

    val config = FieldConfiguration(List(Field("f2", 2.0f, 2), Field("f1", 1, 1)))

    configAccessor.store(config)

    val readConfig = configAccessor.load

    assertEquals(config, readConfig)
  }
}
