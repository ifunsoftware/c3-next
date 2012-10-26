package org.aphreet.c3.platform.search.impl

import org.aphreet.c3.platform.search.{HandleFieldListMsg, SearchConfigurationManager}
import org.springframework.stereotype.Component
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.search.ext.SearchConfiguration
import scala.collection.JavaConversions._
import collection.immutable.SortedMap

@Component
class SearchConfigurationManagerImpl extends SearchConfigurationManager{

  @Autowired
  var configAccessor:SearchConfigurationAccessor = _

  var currentSearchConfiguration = new SearchConfiguration

  var currentFields:FieldConfiguration = _

  @PostConstruct
  def init(){

    currentFields = configAccessor.load

    currentSearchConfiguration.loadFieldWeight(mapAsJavaMap(currentFields.fieldMap))

    this.start()
  }

  def searchConfiguration:SearchConfiguration = {
    currentSearchConfiguration
  }


  def act() {
    loop {
      react{
        case HandleFieldListMsg(fields) => {
          currentFields = currentFields.handleIndexedFields(fields)
          currentSearchConfiguration.loadFieldWeight(mapAsJavaMap(currentFields.fieldMap))
          configAccessor.store(currentFields)
        }
        case DestroyMsg => this.exit()
      }
    }
  }

  @PreDestroy
  def destroy(){
    this ! DestroyMsg
  }
}
case class FieldConfiguration(fields:List[Field]){

  def handleIndexedFields(newFields:List[String]):FieldConfiguration = {

    var newConfiguration = fields.map(f => (f.name, f)).toMap

    for (name <- newFields){
      newConfiguration.get(name) match {
        case Some(field) => newConfiguration = newConfiguration + ((name, field.incCount()))
        case None => newConfiguration = newConfiguration + ((name, Field(name, 1, 1)))
      }
    }

    FieldConfiguration(newConfiguration.values.toList.sortWith(Field.lt))
  }

  def fieldMap:Map[String, java.lang.Float] = {
    fields.map(f => (f.name, new java.lang.Float(f.weight))).toMap
  }

}

case class Field(name:String, weight:Float, count:Int){

  def incCount():Field = {
    Field(name, weight, count + 1)
  }
}

object Field{
  def lt (f1:Field, f2:Field):Boolean = f1.count < f2.count
}

