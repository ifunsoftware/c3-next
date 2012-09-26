package org.aphreet.c3.platform.zone

case class ZoneConfig(timeRanges:List[TimeRangeConfig]){

  def createZoneSet:ZoneSet = {
    new ZoneSet(new RangeSet(timeRanges.reverse.map(_.createTimeRange)))
  }

  def addTimeRange(timeRange:TimeRangeConfig):ZoneConfig = {

    timeRanges.headOption match {
      case Some(oldRange) => {
        ZoneConfig(timeRange :: oldRange.updateEndTime(timeRange.start - 1) :: timeRanges.tail)
      }
      case None =>
        ZoneConfig(List(timeRange))
    }
  }

}

case class TimeRangeConfig(start:Long, end:Long, idRanges:List[IdRange]){

  def createTimeRange:TimeRange = {
    TimeRange(start, end, new RangeSet[Zone](idRanges))
  }

  def updateEndTime(newEnd:Long):TimeRangeConfig = {
    TimeRangeConfig(start, newEnd, idRanges)
  }

}
