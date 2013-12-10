package org.aphreet.c3.platform.storage.impl

import com.springsource.json.parser.{MapNode, Node}
import com.springsource.json.writer.JSONWriter
import java.util.{List => JList}
import org.aphreet.c3.platform.config.SystemDirectoryProvider
import org.aphreet.c3.platform.storage.StorageIndex
import org.aphreet.c3.platform.storage.StorageIndexConfigAccessor
import scala.List
import scala.collection.JavaConversions._

class StorageIndexConfigAccessorImpl(val directoryConfigProvider: SystemDirectoryProvider)
  extends StorageIndexConfigAccessor{

  protected def configFileName = "c3-storage-index-config.json"

  protected def defaultConfig = List(
    new StorageIndex("domain_idx", List("c3.domain.id"), system=true, multi=false, created=0l),
    new StorageIndex("created_idx", List("created"), system=true, multi=false, created=0l),
    new StorageIndex("updated_idx", List("updated"), system=true, multi=false, created=0l)
  )

  def writeConfig(data: List[StorageIndex], writer: JSONWriter) {
    writer.`object`

    writer.key("indexes").array //indexes start
    for(index <- data){
      writer.`object`
        .key("name").value(index.name)
        .key("multi").value(index.multi)
        .key("system").value(index.system)
        .key("created").value(index.created)
        .key("fields").array
      for(field <- index.fields)
        writer.value(field)
      writer.endArray
      writer.endObject
    }
    writer.endArray()
    writer.endObject()
  }

  def readConfig(node: Node): List[StorageIndex] = {

    var indexes:List[StorageIndex] = List()

    val indexesNode = node.getNode("indexes")

    if(indexesNode != null){

      val indexMaps = asScalaBuffer(
        indexesNode.getNodes.asInstanceOf[JList[MapNode]])

      for (indexMap <- indexMaps){
        val indexName = indexMap.getNode("name").getValue[String]
        val mulIndex =  indexMap.getNode("multi").getValue[Boolean]
        val system = indexMap.getNode("system").getValue[Boolean]
        val created:Long = indexMap.getNode("created").getValue[String].toLong

        val fieldList =
          indexMap.getNode("fields").getNodes.map(_.getValue[String]).toList

        indexes = indexes ::: List(new StorageIndex(indexName, fieldList, mulIndex, system, created))
      }
    }

    indexes
  }
}
