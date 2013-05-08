package org.aphreet.c3.platform.remote.rest.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestHeader, RequestMethod, RequestMapping}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.config.VersionManager
import org.aphreet.c3.platform.remote.rest.response.StatusResult

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
@Controller
class StatusController extends DataController {

  @Autowired
  var versionManager: VersionManager = _

  @RequestMapping(value = Array("/status"),
    method = Array(RequestMethod.GET),
    produces = Array("application/json", "application/xml"))
  def executeQuery(req: HttpServletRequest,
                   resp: HttpServletResponse) {

    getResultWriter(req).writeResponse(new StatusResult(
      SystemStatus(versionManager.listC3Modules.map{case (name, version) => SystemModule(name, version)}.toArray.sortBy(_.name))
    ), resp)
  }
}

case class SystemModule(name: String, version: String)

case class SystemStatus(modules: Array[SystemModule])


