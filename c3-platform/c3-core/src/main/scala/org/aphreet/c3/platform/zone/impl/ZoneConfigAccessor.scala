package org.aphreet.c3.platform.zone.impl

import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigAccessor}
import org.aphreet.c3.platform.zone.{Zone, IdRange, TimeRangeConfig, ZoneConfig}
import java.io.{StringWriter, File}
import org.springframework.beans.factory.annotation.Autowired
import com.springsource.json.writer.JSONWriterImpl
import org.aphreet.c3.platform.common.JSONFormatter
import com.springsource.json.parser.{ScalarNode, ListNode, MapNode, AntlrJSONParser}
import org.springframework.stereotype.Component

@Component
class ZoneConfigAccessor extends ConfigAccessor[ZoneConfig]{

  @Autowired
  var configManager: PlatformConfigManager = null

  def configDir: File = configManager.configDir

  protected def configFileName = "c3-zones.json"

  protected def defaultConfig = ZoneConfig(Nil)

  def loadConfig(configFile: File):ZoneConfig = {

    val node = new AntlrJSONParser().parse(configFile)
    val timeRangesArray = node.asInstanceOf[MapNode].getNode("timeranges").asInstanceOf[ListNode].getNodes.toArray

    val timeRanges = timeRangesArray.map(timeRange => {
      val timeRangeNode = timeRange.asInstanceOf[MapNode]

      val start = timeRangeNode.getNode("start").asInstanceOf[ScalarNode].getValue[String].toLong
      val end = timeRangeNode.getNode("end").asInstanceOf[ScalarNode].getValue[String].toLong

      val idRanges = timeRangeNode.getNode("idRanges").asInstanceOf[ListNode].getNodes.toArray.map(idRange => {
        val idRangeNode = idRange.asInstanceOf[MapNode]

        val start = idRangeNode.getNode("start").asInstanceOf[ScalarNode].getValue[String].toLong
        val end = idRangeNode.getNode("end").asInstanceOf[ScalarNode].getValue[String].toLong

        val storages = idRangeNode.getNode("zone").asInstanceOf[MapNode].getNode("storages").asInstanceOf[ListNode].getNodes.toArray
          .map(storageNode => storageNode.asInstanceOf[ScalarNode].getValue[String]).toList

        IdRange(start, end, Zone(storages))
      }).toList

      TimeRangeConfig(start, end, idRanges)

    }).toList


    ZoneConfig(timeRanges)
  }

  def storeConfig(data: ZoneConfig, configFile: File) {
    this.synchronized {

          val sWriter = new StringWriter()

      try {
            val writer = new JSONWriterImpl(sWriter)

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

            sWriter.flush()

            val result = JSONFormatter.format(sWriter.toString)

            writeToFile(result, configFile)

          } finally {
            sWriter.close()
          }
        }
  }
}
