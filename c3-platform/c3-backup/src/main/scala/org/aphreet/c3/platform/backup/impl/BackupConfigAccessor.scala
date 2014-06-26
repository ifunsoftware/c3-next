package org.aphreet.c3.platform.backup.impl

import com.springsource.json.parser.{MapNode, Node}
import com.springsource.json.writer.JSONWriter
import org.aphreet.c3.platform.backup.{RemoteBackupLocation, LocalBackupLocation, BackupLocation}
import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.config.{ConfigPersister, ConfigAccessor}
import scala.collection.JavaConversions._
import org.aphreet.c3.platform.exception.ConfigurationException

class BackupConfigAccessor(val persister: ConfigPersister) extends ConfigAccessor[List[BackupLocation]] {

  def name = "c3-backup-config"

  val log = Logger(getClass)

  def defaultConfig: List[BackupLocation] = Nil

  def readConfig(node: Node): List[BackupLocation] = {
    (
      for (locationNode <- node.getNode("targets").getNodes)
      yield createBackupLocation(locationNode)
      ).toList
  }

  private def createBackupLocation(node: MapNode): BackupLocation = {

    def getPort(node: MapNode): String = {
      if (node.getNode("port") != null) {
        node.getNode("port")
      } else {
        "22"
      }
    }

    val backupType: String = node.getNode("type")

    backupType match {
      case "local" => new LocalBackupLocation(
        node.getNode("id"),
        node.getNode("folder"),
        node.getNode("schedule").getNodes.toList.map(e => e.toString.replaceAll("\"", "").trim)
      )
      case "remote" => new RemoteBackupLocation(
        node.getNode("id"),
        node.getNode("host"),
        getPort(node).toInt,
        node.getNode("user"),
        node.getNode("folder"),
        node.getNode("key").toString.replaceAll("\\\\n", "\n").replaceAll("\"", "").trim,
        null,
        node.getNode("schedule").getNodes.toList.map(e => e.toString.replaceAll("\"", "").trim)
      )
      case _ => throw new ConfigurationException("Unknown backup type " + backupType)
    }

  }

  def writeConfig(list: List[BackupLocation], writer: JSONWriter) {

    writer.`object`

    writer.key("targets")

    writer.array

    for (location <- list) {
      writeLocation(location, writer)
    }
    writer.endArray
    writer.endObject
  }

  private def writeLocation(location: BackupLocation, writer: JSONWriter) {

    writer.`object`

    writer.key("id")
    writer.value(location.id)

    writer.key("schedule")
    writer.array()
    if (location.schedule != null) {
      for (s <- location.schedule) {
        writer.value(s)
      }
    }
    writer.endArray()

    location match {
      case x: LocalBackupLocation => {
        writer.key("type")
        writer.value("local")
        writer.key("folder")
        writer.value(x.directory)
      }
      case x: RemoteBackupLocation => {
        writer.key("type")
        writer.value("remote")
        writer.key("host")
        writer.value(x.host)
        writer.key("port")
        writer.value(x.port.toString)
        writer.key("user")
        writer.value(x.user)
        writer.key("folder")
        writer.value(x.folder)
        writer.key("key")
        writer.value(x.privateKey)
      }
    }
    writer.endObject
  }
}
