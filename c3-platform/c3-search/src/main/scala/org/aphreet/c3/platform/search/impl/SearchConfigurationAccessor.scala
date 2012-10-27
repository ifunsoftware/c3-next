package org.aphreet.c3.platform.search.impl

import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigAccessor}
import org.springframework.stereotype.Component
import java.io.{StringWriter, File}
import org.springframework.beans.factory.annotation.Autowired
import com.springsource.json.writer.JSONWriterImpl
import org.aphreet.c3.platform.common.JSONFormatter
import com.springsource.json.parser.{ListNode, ScalarNode, MapNode, AntlrJSONParser}
import collection.JavaConversions._
import collection.mutable.ArrayBuffer

@Component
class SearchConfigurationAccessor extends ConfigAccessor[FieldConfiguration]{

  @Autowired
  var configManager:PlatformConfigManager = _

  protected def configFileName = "c3-search-config.json"

  protected def configDir = configManager.configDir

  protected def defaultConfig = FieldConfiguration(List())

  def loadConfig(configFile: File):FieldConfiguration = {
    val buffer = new ArrayBuffer[Field]

    val node = new AntlrJSONParser().parse(configFile).asInstanceOf[ListNode]

    for(fieldValues <- asScalaBuffer(node.getNodes)){
      val array = fieldValues.asInstanceOf[ListNode]

      val name = array.getNodes.get(0).asInstanceOf[ScalarNode].getValue[String]
      val weight = array.getNodes.get(1).asInstanceOf[ScalarNode].getValue[String].toFloat
      val count = array.getNodes.get(2).asInstanceOf[ScalarNode].getValue[String].toInt

      buffer.add(Field(name, weight, count))
    }

    FieldConfiguration(buffer.toList)
  }

  def storeConfig(data: FieldConfiguration, configFile: File) {
    this.synchronized {
      val swriter = new StringWriter()

      try {
        val writer = new JSONWriterImpl(swriter)

        writer.array()

        for(field <- data.fields){
          writer.array()
          writer.value(field.name)
          writer.value(field.weight)
          writer.value(field.count)
          writer.endArray()
        }

        writer.endArray()

        swriter.flush()

        val result = JSONFormatter.format(swriter.toString)

        writeToFile(result, configFile)

      } finally {
        swriter.close()
      }
    }
  }
}
