package org.aphreet.c3.platform.zone.impl

import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigAccessor}
import org.aphreet.c3.platform.zone.{Zone, IdRange, TimeRangeConfig, ZoneConfig}
import java.io.{StringWriter, File}
import org.springframework.beans.factory.annotation.Autowired
import com.springsource.json.writer.{JSONWriter, JSONWriterImpl}
import org.aphreet.c3.platform.common.JSONFormatter
import com.springsource.json.parser._
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.zone.ZoneConfig
import org.aphreet.c3.platform.zone.TimeRangeConfig

@Component
class ZoneConfigAccessor extends ConfigAccessor[ZoneConfig]{

  @Autowired
  var configManager: PlatformConfigManager = null

  def configDir: File = configManager.configDir

  protected def configFileName = "c3-zones.json"

  protected def defaultConfig = ZoneConfig(Nil)

  def readConfig(node:Node):ZoneConfig = {

    val timeRangesArray = node.getNode("timeranges").getNodes.toArray

    val timeRanges = timeRangesArray.map(timeRange => {
      val timeRangeNode = timeRange.asInstanceOf[MapNode]

      val start = timeRangeNode.getNode("start").getValue[String].toLong
      val end = timeRangeNode.getNode("end").getValue[String].toLong

      val idRanges = timeRangeNode.getNode("idRanges").getNodes.toArray.map(idRange => {
        val idRangeNode = idRange.asInstanceOf[MapNode]

        val start = idRangeNode.getNode("start").getValue[String].toLong
        val end = idRangeNode.getNode("end").getValue[String].toLong

        val storages = idRangeNode.getNode("zone").getNode("storages").getNodes.toArray
          .map(storageNode => storageNode.asInstanceOf[ScalarNode].getValue[String]).toList

        IdRange(start, end, Zone(storages))
      }).toList

      TimeRangeConfig(start, end, idRanges)

    }).toList


    ZoneConfig(timeRanges)
  }

  def writeConfig(data: ZoneConfig, writer:JSONWriter) {

    writer.`object`.key("timeranges").array

    for (timeRange <- data.timeRanges){
      writer.`object`()
        .key("start").value(timeRange.start)
        .key("end").value(timeRange.end)
        .key("idRanges").array()
      for (idRange <- timeRange.idRanges){
        writer.`object`()
          .key("start").value(idRange.start)
          .key("end").value(idRange.end)
          .key("zone").`object`()
          .key("storages").array()
        for (storage <-idRange.value.storageIds){
          writer.value(storage)
        }
        writer.endArray()
        writer.endObject()
        writer.endObject()
      }
      writer.endArray()
      writer.endObject()
    }
    writer.endArray()
    writer.endObject()

  }
}
