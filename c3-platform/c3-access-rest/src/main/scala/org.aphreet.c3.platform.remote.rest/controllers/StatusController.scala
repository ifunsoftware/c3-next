package org.aphreet.c3.platform.remote.rest.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestHeader, RequestMethod, RequestMapping}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.config.VersionManager

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
@Controller
class StatusController extends DataController {

  @Autowired
  var versionManager: VersionManager = _

  @RequestMapping(value = Array("/version"), method = Array(RequestMethod.GET))
  def executeQuery(req: HttpServletRequest,
                   resp: HttpServletResponse,
                   @RequestHeader(value = "x-c3-type", required = false) contentType: String) {
    val writer = resp.getWriter

    writer.println("[")
    val versionLines = versionManager.listC3Modules.map {
      case (module, version) => module + ": " + version
    }
    writer.println(versionLines.mkString(",\n"))
    writer.println("]")

    resp.flushBuffer()
  }
}