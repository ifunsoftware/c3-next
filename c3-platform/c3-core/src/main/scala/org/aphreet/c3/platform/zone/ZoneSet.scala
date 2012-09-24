package org.aphreet.c3.platform.zone

import org.aphreet.c3.platform.resource.{IdGenerator, ResourceAddress}

case class ZoneSet(timeRangeSet:RangeSet[RangeSet[Zone]]) {

  def zoneForAddress(address:ResourceAddress):Option[Zone] = {

    timeRangeSet.findMappedValue(address.time) match {
      case Some(range) => {
        range.findMappedValue(addressToIdValue(address))
      }
      case None => None
    }
  }

  def addressToIdValue(address:ResourceAddress):Short = {
    math.abs(IdGenerator.trailShort(address.randomPart)).toShort
  }
}

case class TimeRange(override val start:Long,
                     override val end:Long,
                     override val value:RangeSet[Zone]) extends Range[RangeSet[Zone]](start, end, value)

case class IdRange(override val start:Long,
                   override val end:Long,
                   override val value:Zone) extends Range[Zone](start, end, value)

object IdRange{

  def generate(zones:List[Zone]):List[IdRange] = {

    val numberOfSplits = zones.size

    val splitSize = Short.MaxValue / numberOfSplits

    val rangeList = for (split <- 0 to numberOfSplits - 1)
      yield IdRange(split * splitSize, (split + 1) * splitSize - 1, zones(split))

    rangeList.toList
  }

}