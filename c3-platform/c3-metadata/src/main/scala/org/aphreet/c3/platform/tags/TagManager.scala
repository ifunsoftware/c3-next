package org.aphreet.c3.platform.tags

import org.aphreet.c3.platform.common.WatchedActor
import scala.collection.Map

trait TagManager extends WatchedActor{

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