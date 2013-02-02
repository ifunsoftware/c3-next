package org.aphreet.c3.platform.backup.impl

import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigAccessor}
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import com.springsource.json.parser.Node
import com.springsource.json.writer.JSONWriter
import org.aphreet.c3.platform.backup.RemoteBackupLocation
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope
import scala.collection.JavaConversions._

@Component
@Scope("singleton")
class BackupConfigAccessor extends ConfigAccessor[List[RemoteBackupLocation]] {

  @Autowired
  var configManager: PlatformConfigManager = _

  def configDir: File = configManager.configDir

  def configFileName: String = "c3-backup-config.json"


  def defaultConfig:List[RemoteBackupLocation] = {
    List(RemoteBackupLocation("backup-c3backup.rhcloud.com", "d22b442f096243d499120ff44adfc76a",
      System.getProperty("user.home") +  "/.c3/id_rsa"))
  }

  def readConfig(node: Node): List[RemoteBackupLocation] = {
    (
      for (locationNode <- node.getNode("locations").getNodes)
      yield new RemoteBackupLocation(
        locationNode.getNode("host"),
        locationNode.getNode("user"),
        locationNode.getNode("key_location"))
      ).toList
  }

  def writeConfig(list: List[RemoteBackupLocation], writer: JSONWriter) {

    writer.`object`

    writer.key("locations")

    writer.array

    for (location <- list) {
      writer.`object`

      writer.key("host")
      writer.value(location.host)
      writer.key("user")
      writer.value(location.user)
      writer.key("key_location")
      writer.value(location.privateKeyLocation)
      writer.endObject
    }
    writer.endArray
    writer.endObject
  }
}
