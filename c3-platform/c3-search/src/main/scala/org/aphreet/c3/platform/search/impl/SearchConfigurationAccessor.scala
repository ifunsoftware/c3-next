package org.aphreet.c3.platform.search.impl

import collection.JavaConversions._
import collection.mutable.ArrayBuffer
import com.springsource.json.parser._
import com.springsource.json.writer.JSONWriter
import org.aphreet.c3.platform.config.{ConfigPersister, ConfigAccessor}

class SearchConfigurationAccessor(val persister: ConfigPersister) extends ConfigAccessor[FieldConfiguration]{

  def name = "c3-search-config"

  protected def defaultConfig = FieldConfiguration(List())

  def readConfig(node: Node):FieldConfiguration = {
    val buffer = new ArrayBuffer[Field]

    for(fieldValues <- node.getNodes){
      val array = fieldValues.asInstanceOf[ListNode]

      val name = array.getNodes.get(0).getValue[String].toLowerCase
      val weight = array.getNodes.get(1).getValue[String].toFloat
      val count = array.getNodes.get(2).getValue[String].toInt

      buffer.add(Field(name, weight, count))
    }

    val fields = buffer.toList.groupBy(_.name)
      .map(entry => entry._2.reduceLeft((field0, field) => Field(field0.name, field0.weight, field0.count + field.count)))
      .toList.sortWith(Field.gt)

    FieldConfiguration(fields)
  }

  def writeConfig(data: FieldConfiguration, writer: JSONWriter) {
    writer.array()

    for(field <- data.fields){
      writer.array()
      writer.value(field.name)
      writer.value(field.weight)
      writer.value(field.count)
      writer.endArray()
    }

    writer.endArray()
  }
}
