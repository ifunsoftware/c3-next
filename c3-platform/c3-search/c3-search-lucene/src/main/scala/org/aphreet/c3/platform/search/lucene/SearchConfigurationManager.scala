package org.aphreet.c3.platform.search.lucene

import org.aphreet.c3.platform.common.ActorRefHolder
import org.aphreet.c3.platform.search.lucene.impl.SearchConfiguration

trait SearchConfigurationManager extends ActorRefHolder{

  def searchConfiguration:SearchConfiguration

}

case class HandleFieldListMsg(fields:List[String])

object DropFieldConfiguration
