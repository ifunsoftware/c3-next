package org.aphreet.c3.platform.remote.management.controllers

import org.springframework.web.bind.annotation.{ResponseStatus, ExceptionHandler}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.aphreet.c3.platform.remote.management.model.ErrorModel
import org.aphreet.c3.platform.remote.management.controllers.exception.{WrongRequestException, NotFoundException}
import org.springframework.http.HttpStatus
import org.aphreet.c3.platform.exception.StorageNotFoundException
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.json.MappingJacksonJsonView

abstract class AbstractController {

  @ExceptionHandler(Array(classOf[Exception]))
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  def handleException(e: Exception,
                      request: HttpServletRequest,
                      response: HttpServletResponse): ModelAndView = {

    new ModelAndView(new MappingJacksonJsonView(), "error",
      ErrorModel(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage))
  }

  @ExceptionHandler(Array(classOf[NotFoundException], classOf[StorageNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException(e: Exception,
                              request: HttpServletRequest,
                              response: HttpServletResponse): ModelAndView = {
    new ModelAndView(new MappingJacksonJsonView(), "error",
      ErrorModel(HttpServletResponse.SC_NOT_FOUND, e.getMessage))
  }

  @ExceptionHandler(Array(classOf[WrongRequestException]))
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  def handleWrongRequestException(e: Exception,
                                  request: HttpServletRequest,
                                  response: HttpServletResponse): ModelAndView = {
    new ModelAndView(new MappingJacksonJsonView(), "error",
      ErrorModel(HttpServletResponse.SC_BAD_REQUEST, e.getMessage))
  }

  def openOr[T](option: Option[T], error: String): T = {
    option match {
      case Some(value) => value
      case None => throw new NotFoundException(error)
    }
  }

}
