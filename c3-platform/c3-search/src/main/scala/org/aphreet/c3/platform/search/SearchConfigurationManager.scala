package org.aphreet.c3.platform.search

import ext.SearchConfiguration
import org.aphreet.c3.platform.common.WatchedActor

trait SearchConfigurationManager extends WatchedActor{

  def searchConfiguration:SearchConfiguration

}

case class HandleFieldListMsg(fields:List[String])

