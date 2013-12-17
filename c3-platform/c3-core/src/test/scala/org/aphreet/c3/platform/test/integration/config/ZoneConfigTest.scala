package org.aphreet.c3.platform.test.integration.config

import junit.framework.Assert._
import org.aphreet.c3.platform.zone.impl.ZoneConfigAccessor
import org.aphreet.c3.platform.zone._
import org.aphreet.c3.platform.config.impl.MemoryConfigPersister
import junit.framework.TestCase

class ZoneConfigTest extends TestCase {

  def testConfigPersistence() {

    val configAccessor = new ZoneConfigAccessor(new MemoryConfigPersister)

    val startTime = 1350000000000l

    val timeRange0 = TimeRangeConfig(startTime, startTime + 1000 - 1,
      IdRange.generate(List(Zone("0000")))
    )

    val timeRange1 = TimeRangeConfig(startTime + 1000, startTime + 2000 - 1,
      IdRange.generate(List(Zone("0000"), Zone("0001")))
    )

    val timeRange2 = TimeRangeConfig(startTime + 2000, startTime + 3000 - 1,
      IdRange.generate(List(Zone("0000"), Zone("0001"), Zone("0002")))
    )

    val timeRange3 = TimeRangeConfig(startTime + 3000, startTime + 2000 - 1,
      IdRange.generate(List(Zone("0000"), Zone("0001"), Zone("0002"), Zone("0003")))
    )

    val timeRangeList = List(timeRange0, timeRange1, timeRange2, timeRange3)

    val config = ZoneConfig(timeRangeList)

    configAccessor.store(config)

    val readConfig = configAccessor.load

    assertEquals(config, readConfig)
  }
}
