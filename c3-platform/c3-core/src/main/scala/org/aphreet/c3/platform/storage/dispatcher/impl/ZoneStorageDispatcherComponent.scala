package org.aphreet.c3.platform.storage.dispatcher.impl

import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.config.PlatformConfigComponent
import org.aphreet.c3.platform.resource.ResourceAddress
import org.aphreet.c3.platform.storage.StorageParams
import org.aphreet.c3.platform.storage.dispatcher.{StorageDispatcherComponent, StorageDispatcher}
import org.aphreet.c3.platform.zone._
import org.aphreet.c3.platform.zone.impl.ZoneConfigAccessor
import scala.Some

trait ZoneStorageDispatcherComponent extends StorageDispatcherComponent{

  this: PlatformConfigComponent =>

  val storageDispatcher: StorageDispatcher = new ZoneStorageDispatcher(new ZoneConfigAccessor(configPersister))

  class ZoneStorageDispatcher(val configAccessor: ZoneConfigAccessor) extends StorageDispatcher{

    val log = Logger(classOf[ZoneStorageDispatcherComponent])

    var zoneConfig:ZoneConfig = ZoneConfig(Nil)

    var zoneSet:ZoneSet = zoneConfig.createZoneSet

    var storageParams: List[StorageParams] = Nil

    {

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
        case Some(zone) => storageParams.find(storage => zone.storageIds.contains(storage.id)) match {
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

    def resetDispatcher(sts:List[StorageParams]){
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
}
