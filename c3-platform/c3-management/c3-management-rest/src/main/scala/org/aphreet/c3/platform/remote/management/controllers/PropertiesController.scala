package org.aphreet.c3.platform.remote.management.controllers

import java.util
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.aphreet.c3.platform.remote.management.controllers.request.PropertyUpdateRequest
import org.aphreet.c3.platform.remote.management.model.PropertiesModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestBody, ResponseBody, RequestMethod, RequestMapping}
import scala.collection.JavaConversions

@Controller
@RequestMapping(Array("/properties"))
class PropertiesController extends AbstractController {

  @Autowired
  var platformManagementEndpoint: PlatformManagementEndpoint = _

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def list(): PropertiesModel = {

    val map = new util.HashMap[String, String]()
    map.putAll(JavaConversions.mapAsJavaMap(platformManagementEndpoint.getPlatformProperties))

    PropertiesModel(map)
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  def update(@RequestBody request: PropertyUpdateRequest) {
    platformManagementEndpoint.setPlatformProperty(request.key, request.value)
  }

}
