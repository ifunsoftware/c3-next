package org.aphreet.c3.platform.remote.management.controllers

import java.util
import org.aphreet.c3.platform.domain.{Domain, DomainManager}
import org.aphreet.c3.platform.remote.management.model.DomainModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import scala.collection.JavaConversions._
import org.aphreet.c3.platform.remote.management.controllers.request.DomainUpdateRequest
import org.springframework.http.HttpStatus


@Controller
@RequestMapping(Array("/domain"))
class DomainController extends AbstractController{

  @Autowired
  var domainManager: DomainManager = _

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def list(): util.Collection[DomainModel] = {

    val defaultDomain = domainManager.getDefaultDomainId

    domainManager.domainList.filter(!_.deleted).map(DomainModel(_, defaultDomain))
  }

  @RequestMapping(method = Array(RequestMethod.GET), value = Array("/byname/{name}"))
  @ResponseBody
  def showByName(@PathVariable name: String): DomainModel = {
    DomainModel(findDomain(name), domainManager.getDefaultDomainId)
  }

  @RequestMapping(method = Array(RequestMethod.GET), value = Array("/byid/{id}"))
  @ResponseBody
  def showById(@PathVariable id: String): DomainModel = {
    DomainModel(findById(id), domainManager.getDefaultDomainId)
  }

  @RequestMapping(method = Array(RequestMethod.PUT), value = Array("/byname/{name}"))
  def updateByName(@PathVariable name: String, @RequestBody updateRequest: DomainUpdateRequest): DomainModel = {
    update(findDomain(name), updateRequest)
  }

  @RequestMapping(method = Array(RequestMethod.PUT), value = Array("/byid/{id}"))
  def updateById(@PathVariable id: String, @RequestBody updateRequest: DomainUpdateRequest): DomainModel = {
    update(findById(id), updateRequest)
  }

  @RequestMapping(method = Array(RequestMethod.DELETE), value = Array("/byid/{id}"))
  def deleteById(@PathVariable id: String) {
    domainManager.deleteDomain(findById(id).name)
  }

  @RequestMapping(method = Array(RequestMethod.DELETE), value = Array("/byname/{name}"))
  def deleteByName(@PathVariable name: String) {
    domainManager.deleteDomain(findDomain(name).name)
  }

  @RequestMapping(method = Array(RequestMethod.POST), value = Array("/{name}"))
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  def create(@PathVariable name: String): DomainModel = {
    domainManager.addDomain(name)

    DomainModel(findDomain(name), domainManager.getDefaultDomainId)
  }

  protected def update(domain: Domain, updateRequest: DomainUpdateRequest): DomainModel = {
    if(updateRequest.needsModeUpdate()){
      domainManager.setMode(domain.name, updateRequest.domainMode)
    }

    if(updateRequest.needsKeyAction()){
      updateRequest.keyAction match {
        case "reset" => domainManager.generateKey(domain.name)
        case "remove" => domainManager.removeKey(domain.name)
      }
    }

    if(updateRequest.isDefaultDomain){
      domainManager.setDefaultDomain(domain.id)
    }

    if(updateRequest.needsNameUpdate()){
      domainManager.updateName(domain.name, updateRequest.domainName)
    }

    openOr(domainManager.domainById(domain.id).filterNot(_.deleted).map(DomainModel(_, domainManager.getDefaultDomainId)),
      "Domain with id " + domain.id + " is not found")
  }

  protected def findDomain(name: String): Domain = {
    openOr(domainManager.domainList.filterNot(_.deleted).find(_.name == name),
          "Domain with name " + name + " is not found")
  }

  protected def findById(id: String): Domain = {
    openOr(domainManager.domainById(id).filterNot(_.deleted),
          "Domain with id " + id + " is not found")
  }


}
