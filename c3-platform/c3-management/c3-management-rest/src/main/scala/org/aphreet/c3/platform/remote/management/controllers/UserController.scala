package org.aphreet.c3.platform.remote.management.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.aphreet.c3.platform.remote.management.model.UserModel
import java.util
import org.aphreet.c3.platform.auth.{User, AuthenticationManager}
import org.springframework.beans.factory.annotation.Autowired
import scala.collection.JavaConversions._
import org.aphreet.c3.platform.remote.management.controllers.request.{UserUpdateRequest, UserCreateRequest}
import org.springframework.http.HttpStatus

@Controller
@RequestMapping(Array("/user"))
class UserController extends AbstractController{

  @Autowired
  var authManager: AuthenticationManager = _

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def list(): util.Collection[UserModel] = {
    authManager.list.map(UserModel(_))
  }

  @RequestMapping(method = Array(RequestMethod.GET), value = Array("/{name}"))
  @ResponseBody
  def view(@PathVariable name: String): UserModel = {
    UserModel(findUser(name))
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  def create(@RequestBody request: UserCreateRequest): UserModel = {
    authManager.create(request.name, request.password)

    UserModel(findUser(request.name))
  }

  @RequestMapping(method = Array(RequestMethod.PUT), value = Array("/{name}"))
  @ResponseBody
  def update(@PathVariable name: String, @RequestBody request: UserUpdateRequest): UserModel = {

    authManager.update(request.name, request.password, request.enabled)

    UserModel(findUser(request.name))
  }

  @RequestMapping(method = Array(RequestMethod.DELETE), value = Array("/{name}"))
  def delete(@PathVariable name: String){
    authManager.delete(name)
  }

  private def findUser(name: String): User = {
    openOr(authManager.list.find(_.name == name), "Can't find user with name " + name)
  }

}
