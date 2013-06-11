package org.aphreet.c3.platform.remote.management.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestBody, RequestMapping, ResponseBody, RequestMethod}
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.springframework.beans.factory.annotation.Autowired
import java.util
import org.aphreet.c3.platform.remote.management.model.StorageModel
import scala.collection.JavaConversions._
import org.aphreet.c3.platform.remote.management.controllers.request.StorageCreateRequest

@Controller
@RequestMapping(Array("/storage"))
class StorageController {

  @Autowired
  var platformManagementService: PlatformManagementEndpoint = _

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def list(): util.Collection[StorageModel] = {
    platformManagementService.listStorages
      .map(StorageModel(_))
  }

  @RequestMapping(method = Array(RequestMethod.GET),  value = Array("/types"))
  @ResponseBody
  def listTypes(): util.Collection[String] = {
    platformManagementService.listStorageTypes
  }

  @RequestMapping(method = Array(RequestMethod.POST), consumes=Array("application/json"))
  @ResponseBody
  def create(@RequestBody request: StorageCreateRequest): StorageModel = {
    StorageModel(platformManagementService.createStorage(request.storageType, request.path))
  }
}
