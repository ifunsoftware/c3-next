package org.aphreet.c3.platform.remote.management.controllers

import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ResponseBody, RequestMethod, RequestMapping}
import org.aphreet.c3.platform.remote.management.model.StatisticsModel
import scala.collection.JavaConversions._

@Controller
@RequestMapping(Array("/statistics"))
class StatisticsController {

  @Autowired
  var platformManagementEndpoint: PlatformManagementEndpoint = _

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def view(): StatisticsModel = {
    StatisticsModel(platformManagementEndpoint.statistics)
  }

}
