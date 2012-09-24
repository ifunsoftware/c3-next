package org.aphreet.c3.platform.zone

case class ZoneConfig(timeRanges:List[TimeRangeConfig]){

  def createZoneSet:ZoneSet = {
    new ZoneSet(new RangeSet(timeRanges.map(_.createTimeRange)))
  }

}

case class TimeRangeConfig(start:Long, end:Long, idRanges:List[IdRange]){

  def createTimeRange:TimeRange = {
    TimeRange(start, end, new RangeSet[Zone](idRanges))
  }

}
