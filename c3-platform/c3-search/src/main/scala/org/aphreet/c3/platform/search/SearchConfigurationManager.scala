package org.aphreet.c3.platform.search

import org.aphreet.c3.platform.common.WatchedActor
import org.aphreet.c3.platform.search.impl.SearchConfiguration

trait SearchConfigurationManager extends WatchedActor{

  def searchConfiguration:SearchConfiguration

}

case class HandleFieldListMsg(fields:List[String])

