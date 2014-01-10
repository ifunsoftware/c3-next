package org.aphreet.c3.platform.search

import org.aphreet.c3.platform.search.impl.SearchConfiguration
import org.aphreet.c3.platform.common.ActorRefHolder

trait SearchConfigurationManager extends ActorRefHolder{

  def searchConfiguration:SearchConfiguration

}

case class HandleFieldListMsg(fields:List[String])

object DropFieldConfiguration

