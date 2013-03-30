package org.aphreet.c3.platform.metadata

import org.aphreet.c3.platform.common.WatchedActor
import scala.collection.Map

trait TagManager extends WatchedActor{

}

case class DeleteParentTagMsg(catalogPath: Option[String], tags: Map[String, Int]) {

}
case class AddParentTagMsg(catalogPath: Option[String], tags: Map[String, Int]) {

}
//TODO: remove option for address; listen to updateResource