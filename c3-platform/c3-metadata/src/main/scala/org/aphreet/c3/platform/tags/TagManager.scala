package org.aphreet.c3.platform.tags

import org.aphreet.c3.platform.common.ActorRefHolder
import scala.collection.Map

trait TagManager extends ActorRefHolder {

}

trait TagComponent {

  def tagManager: TagManager

}

object TagManager {

  val TAGS_FIELD = "tags"
}

case class DeleteParentTagMsg(catalogPath: Option[String], tags: Map[String, Int]) {

}
case class AddParentTagMsg(catalogPath: Option[String], tags: Map[String, Int]) {

}
case class RebuildParentTagMsg(catalogPath: Option[String]) {

}
//TODO: remove option for address; listen to updateResource