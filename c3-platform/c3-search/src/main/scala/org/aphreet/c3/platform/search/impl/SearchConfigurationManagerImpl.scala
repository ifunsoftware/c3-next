package org.aphreet.c3.platform.search.impl

import org.aphreet.c3.platform.search.{HandleFieldListMsg, SearchConfigurationManager}
import org.springframework.stereotype.Component
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.search.ext.SearchConfiguration
import scala.collection.JavaConversions._

@Component
class SearchConfigurationManagerImpl extends SearchConfigurationManager{

  @Autowired
  var configAccessor:SearchConfigurationAccessor = _

  var currentSearchConfiguration = new SearchConfiguration

  @PostConstruct
  def init(){

    val fields = configAccessor.load.map(e => (e._1, Integer.valueOf(1))).toMap

    currentSearchConfiguration.loadFieldWeight(mapAsJavaMap(fields))

    this.start()
  }

  def searchConfiguration:SearchConfiguration = {
    currentSearchConfiguration
  }


  def act() {
    loop {
      react{
        case HandleFieldListMsg(fields) => {
          configAccessor
        }
        case DestroyMsg => this.exit()
      }
    }
  }

  @PreDestroy
  def destroy(){
    this ! DestroyMsg
  }
}
