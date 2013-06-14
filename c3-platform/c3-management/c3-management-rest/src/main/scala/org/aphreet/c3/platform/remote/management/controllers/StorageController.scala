package org.aphreet.c3.platform.remote.management.controllers

import java.util
import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.aphreet.c3.platform.remote.management.controllers.exception.{WrongRequestException, NotFoundException}
import org.aphreet.c3.platform.remote.management.controllers.request.StorageCreateRequest
import org.aphreet.c3.platform.remote.management.controllers.request.StorageUpdateRequest
import org.aphreet.c3.platform.remote.management.model.StorageModel
import org.aphreet.c3.platform.storage.{U, RO, RW}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import scala.Some
import scala.collection.JavaConversions._

@Controller
@RequestMapping(Array("/storage"))
class StorageController extends AbstractController{

  @Autowired
  var platformManagementService: PlatformManagementEndpoint = _

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def list(): util.Collection[StorageModel] = {
    platformManagementService.listStorages
      .map(StorageModel(_))
  }

  @RequestMapping(method = Array(RequestMethod.GET), value = Array("/{id}"))
  @ResponseBody
  def view(@PathVariable id: String): StorageModel = {
    platformManagementService.listStorages.find(_.id == id) match {
      case Some(storage) => StorageModel(storage)
      case None => {
        throw new NotFoundException("Can't find storage with id " + id)
      }
    }
  }

  @RequestMapping(method = Array(RequestMethod.GET),  value = Array("/types"))
  @ResponseBody
  def listTypes(): util.Collection[String] = {
    platformManagementService.listStorageTypes
  }

  @RequestMapping(method = Array(RequestMethod.POST), consumes=Array("application/json"))
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  def create(@RequestBody request: StorageCreateRequest): StorageModel = {
    StorageModel(platformManagementService.createStorage(request.storageType, request.path))
  }

  @RequestMapping(method = Array(RequestMethod.DELETE), value = Array("/{id}"))
  def delete(@PathVariable id: String) {
    platformManagementService.removeStorage(id)
  }

  @RequestMapping(method = Array(RequestMethod.PUT), value = Array("/{id}"))
  def update(@PathVariable id: String, @RequestBody request: StorageUpdateRequest) {

    val storageMode = request.mode match {
      case "RW" => RW(STORAGE_MODE_USER)
      case "RO" => RO(STORAGE_MODE_USER)
      case "U" => U(STORAGE_MODE_USER)
      case _ => throw new WrongRequestException("No mode named " + request.mode)
    }

    platformManagementService.setStorageMode(id, storageMode)
  }
}
