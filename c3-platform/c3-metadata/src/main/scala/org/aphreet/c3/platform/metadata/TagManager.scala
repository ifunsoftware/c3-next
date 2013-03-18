package org.aphreet.c3.platform.metadata

import org.aphreet.c3.platform.common.WatchedActor


trait TagManager extends WatchedActor{

  def updateParentTag(catalogPath:String)

}

case class UpdateParentTagMsg(catalogPath: Option[String], tags: List[String]) {

}