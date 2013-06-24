package org.aphreet.c3.platform.remote.management.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestBody, ResponseBody, RequestMethod, RequestMapping}
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.remote.management.model.PropertiesModel
import org.aphreet.c3.platform.remote.management.controllers.request.PropertyUpdateRequest

@Controller
@RequestMapping(Array("/properties"))
class PropertiesController extends AbstractController{

  @Autowired
  var platformManagementEndpoint: PlatformManagementEndpoint = _

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def list(): PropertiesModel = {
    PropertiesModel(platformManagementEndpoint.getPlatformProperties)
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  def update(@RequestBody request: PropertyUpdateRequest){
    platformManagementEndpoint.setPlatformProperty(request.key, request.value)
  }

}
