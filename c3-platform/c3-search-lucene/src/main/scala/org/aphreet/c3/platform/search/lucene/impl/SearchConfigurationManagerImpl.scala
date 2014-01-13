package org.aphreet.c3.platform.search.lucene.impl

import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.config.ConfigAccessor
import org.aphreet.c3.platform.search.lucene.{DropFieldConfiguration, HandleFieldListMsg, SearchConfigurationManager}


class SearchConfigurationManagerImpl(val configAccessor: ConfigAccessor[FieldConfiguration]) extends SearchConfigurationManager {

  var currentSearchConfiguration = new SearchConfiguration

  var currentFields: FieldConfiguration = _

  {
    currentFields = configAccessor.load

    currentSearchConfiguration.loadFieldWeight(currentFields.fieldMap)

    this.start()
  }

  def searchConfiguration: SearchConfiguration = {
    currentSearchConfiguration
  }


  def act() {
    loop {
      react {
        case HandleFieldListMsg(fields) => {
          currentFields = currentFields.handleIndexedFields(fields)
          currentSearchConfiguration.loadFieldWeight(currentFields.fieldMap)
          configAccessor.store(currentFields)
        }

        case DropFieldConfiguration => {
          currentFields = FieldConfiguration(List())
          currentSearchConfiguration.loadFieldWeight(currentFields.fieldMap)
          configAccessor.store(currentFields)
        }

        case DestroyMsg => this.exit()
      }
    }
  }

  def destroy() {
    this ! DestroyMsg
  }
}

case class FieldConfiguration(fields: List[Field]) {

  def handleIndexedFields(newFields: List[String]): FieldConfiguration = {

    var newConfiguration = fields.map(f => (f.name, f)).toMap

    for (name <- newFields) {
      newConfiguration.get(name) match {
        case Some(field) => newConfiguration = newConfiguration + ((name.toLowerCase, field.incCount()))
        case None => newConfiguration = newConfiguration + ((name.toLowerCase, Field(name, 1, 1)))
      }
    }

    FieldConfiguration(newConfiguration.values.toList.sortWith(Field.gt))
  }

  def fieldMap: Map[String, java.lang.Float] = {
    fields.map(f => (f.name, new java.lang.Float(f.weight))).toMap
  }

}

case class Field(name: String, weight: Float, count: Int) {

  def incCount(): Field = {
    Field(name, weight, count + 1)
  }
}

object Field {
  def gt(f1: Field, f2: Field): Boolean = f1.count > f2.count
}

