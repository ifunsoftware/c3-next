package org.aphreet.c3.platform.storage.dispatcher.impl

import org.aphreet.c3.platform.storage.dispatcher.StorageDispatcher
import org.aphreet.c3.platform.resource.{ResourceAddress, Resource}
import org.aphreet.c3.platform.storage.{StorageParams, Storage}
import org.aphreet.c3.platform.zone._
import impl.ZoneConfigAccessor
import org.aphreet.c3.platform.zone.ZoneConfig
import scala.Some
import org.aphreet.c3.platform.zone.ZoneSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import org.aphreet.c3.platform.exception.StorageNotFoundException
import org.apache.commons.logging.LogFactory

@Component
class ZoneStorageDispatcher extends StorageDispatcher{

  val log = LogFactory.getLog(getClass)

  @Autowired
  var configAccessor:ZoneConfigAccessor = null

  var zoneConfig:ZoneConfig = ZoneConfig(Nil)

  var zoneSet:ZoneSet = zoneConfig.createZoneSet

  var storageParams: List[StorageParams] = Nil

  @PostConstruct
  def init(){

    log.info("Loading zones configuration")

    zoneConfig = configAccessor.load
    zoneSet = zoneConfig.createZoneSet

    log.info("Current zone configuration is " + zoneConfig)
  }


  def setStorageParams(sts: List[StorageParams]) {
    synchronized {
      this.storageParams = sts
      updateZoneSet(storageParams)
    }
  }

  def selectStorageForAddress(resourceAddress: ResourceAddress):Option[StorageParams] = {
    zoneSet.zoneForAddress(resourceAddress) match {
      case Some(zone) => storageParams.filter(storage => zone.storageIds.contains(storage.id)).headOption match {
        case None => None
        case params => params
      }
      case None => None
    }
  }

  def mergeStorages(fromId:String, toId:String) {
    synchronized{
      val newZoneConfig = zoneConfig.replaceStorageId(fromId, toId)
      configAccessor.store(newZoneConfig)
      zoneConfig = newZoneConfig
      zoneSet = newZoneConfig.createZoneSet
    }
  }

  def reset(sts:List[StorageParams]){
    synchronized{
      zoneConfig = ZoneConfig(List())
      updateZoneSet(sts)
    }
  }

  protected def updateZoneSet(storageParams:List[StorageParams]){
    val writableStorageIds = storageParams.filter(s => s.mode.allowWrite).map(s => s.id).toSet

    val previousStorageIds = zoneConfig.timeRanges.headOption match {
      case Some(timeRange) =>
        timeRange.idRanges.map(idRange => idRange.value.storageIds).reduceLeft(_ ::: _).toSet
      case None => Set()
    }

    if(writableStorageIds != previousStorageIds
      && !writableStorageIds.isEmpty){

      log.info("Changed writtable storage set from " + previousStorageIds + " to " + writableStorageIds)

      val idRanges = IdRange.generate(writableStorageIds.map(Zone(_)).toList)

      val newTimeRange = TimeRangeConfig(System.currentTimeMillis() + 1000, Long.MaxValue, idRanges)

      log.info("Created new time range " + newTimeRange )

      val newZoneConfig = zoneConfig.addTimeRange(newTimeRange)

      configAccessor.store(newZoneConfig)
      zoneConfig = newZoneConfig
      zoneSet = newZoneConfig.createZoneSet

      log.info("Update working zoneset")
    }
  }
}
