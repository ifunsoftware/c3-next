package org.aphreet.c3.platform.storage.dispatcher.impl

import org.aphreet.c3.platform.storage.dispatcher.StorageDispatcher
import org.aphreet.c3.platform.resource.{ResourceAddress, Resource}
import org.aphreet.c3.platform.storage.Storage
import org.aphreet.c3.platform.zone._
import impl.ZoneConfigAccessor
import org.aphreet.c3.platform.zone.ZoneConfig
import scala.Some
import org.aphreet.c3.platform.zone.ZoneSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class ZoneStorageDispatcher extends StorageDispatcher{

  @Autowired
  var configAccessor:ZoneConfigAccessor = null

  var zoneConfig:ZoneConfig = ZoneConfig(Nil)

  var zoneSet:ZoneSet = zoneConfig.createZoneSet

  var storages: List[Storage] = Nil

  @PostConstruct
  def init(){
    zoneConfig = configAccessor.load
    zoneSet = zoneConfig.createZoneSet
  }


  def setStorages(sts: List[Storage]) {
    synchronized {
      this.storages = storages
      updateZoneSet(storages)
    }
  }

  def selectStorageForResource(resource: Resource) = {
    zoneSet.zoneForAddress(ResourceAddress(resource.address)) match {
      case Some(zone) => storages.filter(storage => zone.storageIds.contains(storage.id)).headOption match {
        case Some(storage) => storage
        case None => null
      }
      case None => null
    }
  }

  protected def updateZoneSet(storages:List[Storage]){
    val writableStorageIds = storages.filter(s => s.mode.allowWrite).map(s => s.id).toSet

    val previousStorageIds = zoneConfig.timeRanges.headOption match {
      case Some(timeRange) =>
        timeRange.idRanges.map(idRange => idRange.value.storageIds).reduceLeft(_ ::: _).toSet
      case None => Set()
    }

    if(writableStorageIds != previousStorageIds){

      val idRanges = IdRange.generate(writableStorageIds.map(Zone(_)).toList)

      val newTimeRange = TimeRangeConfig(System.currentTimeMillis(), Long.MaxValue, idRanges)

      val newZoneConfig = zoneConfig.addTimeRange(newTimeRange)

      configAccessor.store(newZoneConfig)
      zoneSet = newZoneConfig.createZoneSet
    }
  }
}
