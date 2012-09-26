package org.aphreet.c3.platform.test.unit

import org.aphreet.c3.platform.zone._
import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.zone.Zone
import scala.Some
import org.aphreet.c3.platform.resource.ResourceAddress

class ZoneSetTestCase  extends TestCase
{

  def testRangeLookup(){

    val ranges = List(StringRange(1, 4,  "value0"),
      StringRange(5, 9, "value1"),
      StringRange(10, 14, "value2"),
      StringRange(15, 19, "value3"),
      StringRange(20, 24, "value4"),
      StringRange(25, 29, "value5"))

    val rangeSet = new RangeSet(ranges)

    assertEquals(None, rangeSet.findMappedValue(-1))
    assertEquals(Some("value0"), rangeSet.findMappedValue(1))
    assertEquals(Some("value0"), rangeSet.findMappedValue(2))
    assertEquals(Some("value2"), rangeSet.findMappedValue(11))
    assertEquals(Some("value1"), rangeSet.findMappedValue(5))
    assertEquals(Some("value4"), rangeSet.findMappedValue(22))
    assertEquals(Some("value4"), rangeSet.findMappedValue(24))
    assertEquals(Some("value5"), rangeSet.findMappedValue(25))
    assertEquals(None, rangeSet.findMappedValue(30))

  }

  def testZoneLookup(){

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

    val timeRangeList = List(timeRange3, timeRange2, timeRange1, timeRange0)

    val config = ZoneConfig(timeRangeList)

    val zoneSet = config.createZoneSet

    assertEquals(Some(Zone("0000")), zoneSet.zoneForAddress(ResourceAddress("12341234","rZ1L9jbMHZgqCvT8gNk3u5iC", startTime)))
    assertEquals(Some(Zone("0000")), zoneSet.zoneForAddress(ResourceAddress("12341234","111L9jbMHZgqCvT8gNk3u5iC", startTime + 1500)))
    assertEquals(Some(Zone("0001")), zoneSet.zoneForAddress(ResourceAddress("12341234","ZZZL9jbMHZgqCvT8gNk3u5iC", startTime + 1500)))

    assertEquals(Some(Zone("0000")), zoneSet.zoneForAddress(ResourceAddress("12341234","111L9jbMHZgqCvT8gNk3u5iC", startTime + 2500)))
    assertEquals(Some(Zone("0002")), zoneSet.zoneForAddress(ResourceAddress("12341234","ZZZL9jbMHZgqCvT8gNk3u5iC", startTime + 2500)))
  }

}

case class StringRange(override val start:Long, override val end:Long, override val value:String) extends Range[String](start, end, value){

}
