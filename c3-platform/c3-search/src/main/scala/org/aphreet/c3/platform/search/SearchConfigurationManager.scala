package org.aphreet.c3.platform.search

import org.aphreet.c3.platform.common.WatchedActor
import collection.immutable.SortedMap

trait SearchConfigurationManager extends WatchedActor{

}

case class HandleFieldListMsg(fields:List[String])

case class FieldConfiguration(fields:SortedMap[String, Field]){

  def handleIndexedFields(newFields:List[String]):FieldConfiguration = {

    var newConfiguration = fields

    for (name <- newFields){
      fields.get(name) match {
        case Some(field) => newConfiguration = newConfiguration + ((name, field.incCount()))
        case None => newConfiguration = newConfiguration + ((name, Field(name, 1, 1)))
      }
    }

    FieldConfiguration(newConfiguration)
  }

}

case class Field(name:String, weight:Float, count:Int){

  def < (other:Field):Boolean = count < other.count

  def incCount():Field = {
    Field(name, weight, count + 1)
  }

}