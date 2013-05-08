package org.aphreet.c3.platform.remote.rest.controllers

import javax.servlet.http.HttpServletRequest
import org.aphreet.c3.platform.accesscontrol.{AccessTokens, Action}
import org.aphreet.c3.platform.remote.rest.response.ResultWriter

trait RestController {

  def getAccessTokens(action: Action, request: HttpServletRequest): AccessTokens

  def getResultWriter(expectedType: String): ResultWriter

  def getResultWriter(request: HttpServletRequest): ResultWriter

}

object RestController {

  val SUPPORTED_CONTENT_TYPES = Array("application/json", "application/xml")

}
