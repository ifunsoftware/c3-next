package org.aphreet.c3.platform.backup.impl

import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigAccessor}
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import com.springsource.json.parser.Node
import com.springsource.json.writer.JSONWriter
import org.aphreet.c3.platform.backup.BackupLocation
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope
import scala.collection.JavaConversions._
import org.apache.commons.logging.LogFactory

@Component
@Scope("singleton")
class BackupConfigAccessor extends ConfigAccessor[List[BackupLocation]] {

  @Autowired
  var configManager: PlatformConfigManager = _

  def configDir: File = configManager.configDir

  def configFileName: String = "c3-backup-config.json"

  val log = LogFactory getLog getClass


  def defaultConfig:List[BackupLocation] = {
    List(BackupLocation("1", "remote", "backup-c3backup.rhcloud.com",
      "d22b442f096243d499120ff44adfc76a", "app-root/data", ""))
  }

  def readConfig(node: Node): List[BackupLocation] = {
    (
      for (locationNode <- node.getNode("targets").getNodes)
      yield new BackupLocation(
        locationNode.getNode("id"),
        locationNode.getNode("type"),
        locationNode.getNode("host"),
        locationNode.getNode("user"),
        locationNode.getNode("folder"),
        locationNode.getNode("key").toString().replaceAll("\\\\n", "\n").replaceAll("\"", "").trim)
    ).toList
  }

  def writeConfig(list: List[BackupLocation], writer: JSONWriter) {

    writer.`object`

    writer.key("targets")

    writer.array

    for (location <- list) {
      writer.`object`

      writer.key("id")
      writer.value(location.id)
      writer.key("type")
      writer.value(location.backupType)
      writer.key("host")
      writer.value(location.host)
      writer.key("user")
      writer.value(location.user)
      writer.key("folder")
      writer.value(location.folder)
      writer.key("key")
      writer.value(location.privateKey)

      writer.endObject
    }
    writer.endArray
    writer.endObject
  }
}
