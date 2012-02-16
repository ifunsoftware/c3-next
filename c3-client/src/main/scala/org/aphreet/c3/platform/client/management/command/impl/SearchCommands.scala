package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.{Command, Commands}
import org.aphreet.c3.platform.remote.api.management.Pair


/**
 * Copyright iFunSoftware 2012
 * @author Mikhail Malygin
 */
;

object SearchCommands extends Commands{

  def instances = List(new SetupSearchCommand)

}

class SetupSearchCommand extends Command{

  def getValue(array:Array[Pair], key:String):String = {
    array.filter(_.key == key).headOption match {
      case Some(value) => value.value
      case None => ""
    }
  }

  def setProperty(key:String, value:String) {
    if(!value.isEmpty)
    management.setPlatformProperty(key, value)
  }

  def execute():String = {

    val properties = management.platformProperties

    print("Search index location [" + getValue(properties, "c3.search.index.path") + "]: ")
    val indexLocation = readInput.trim

    print("Number of indexer threads [" + getValue (properties, "c3.search.index.count") + "]: ")
    val indexerNumber = readNumber

    print("Max number of documents in temp index [" + getValue (properties, "c3.search.index.max_size") + "]: ")
    val maxIndexSize = readNumber

    print("Extract content of the documents [" + getValue (properties, "c3.search.index.extract_content") + "]: ")
    val extractDocumentContent = readBoolean

    setProperty("c3.search.index.path", indexLocation)
    setProperty("c3.search.index.count", indexerNumber)
    setProperty("c3.search.index.max_size", maxIndexSize)
    setProperty("c3.search.index.extract_content", extractDocumentContent)

    "Done"
  }

  def name = List("set", "search")
}