package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.{InteractiveCommand, Command, Commands}
import org.aphreet.c3.platform.remote.api.management.{PlatformManagementService, Pair}
import jline.ConsoleReader


/**
 * Copyright iFunSoftware 2012
 * @author Mikhail Malygin
 */
;

object SearchCommands extends Commands{

  def instances = List(new SetupSearchCommand)

}

class SetupSearchCommand extends InteractiveCommand{

  def getValue(array:Array[Pair], key:String):String = {
    array.filter(_.key == key).headOption match {
      case Some(value) => value.value
      case None => ""
    }
  }

  def setProperty(management:PlatformManagementService, key:String, value:String) {
    if(!value.isEmpty)
      management.setPlatformProperty(key, value)
  }

  override
  def execute(params:List[String], management:PlatformManagementService, reader:ConsoleReader):String = {

    val properties = management.platformProperties

    print("Search index location [" + getValue(properties, "c3.search.index.path") + "]: ")
    val indexLocation = readInput(reader).trim

    print("Number of indexer threads [" + getValue (properties, "c3.search.index.count") + "]: ")
    val indexerNumber = readNumber(reader)

    print("Max number of documents in temp index [" + getValue (properties, "c3.search.index.max_size") + "]: ")
    val maxIndexSize = readNumber(reader)

    print("Extract content of the documents [" + getValue (properties, "c3.search.index.extract_content") + "]: ")
    val extractDocumentContent = readBoolean(reader)

    setProperty(management, "c3.search.index.path", indexLocation)
    setProperty(management, "c3.search.index.count", indexerNumber)
    setProperty(management, "c3.search.index.max_size", maxIndexSize)
    setProperty(management, "c3.search.index.extract_content", extractDocumentContent)

    "Done"
  }

  def name = List("set", "search")
}