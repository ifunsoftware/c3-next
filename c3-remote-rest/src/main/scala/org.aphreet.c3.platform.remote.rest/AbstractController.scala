/**
 * Copyright (c) 2011, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *

 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.platform.remote.rest

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.aphreet.c3.platform.exception.ResourceNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import response.{ResultWriter, ResultWriterSelector, ErrorResult, ErrorDescription}
import org.springframework.web.bind.annotation.ExceptionHandler
import org.aphreet.c3.platform.auth.exception.AuthFailedException
import org.aphreet.c3.platform.domain.DomainException
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.filesystem.{FSNotFoundException, FSWrongRequestException}
import org.springframework.web.HttpRequestMethodNotSupportedException

class AbstractController{

  val log = LogFactory getLog getClass

  var writerSelector:ResultWriterSelector = _

  @Autowired
  def setResultWriterSelector(selector:ResultWriterSelector) {
    writerSelector = selector
  }

  @ExceptionHandler(Array(classOf[Exception]))
  def handleException(e:Exception,
                      request:HttpServletRequest,
                      response:HttpServletResponse) {

    val contentType = request.getHeader("x-c3-type")
    
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)

    getResultWriter(contentType).writeResponse(new ErrorResult(new ErrorDescription("Internal Server Error", e)), response)
  }

  def handleUnsupportedMethodException(e:HttpRequestMethodNotSupportedException,
                                       request:HttpServletRequest,
                                       response:HttpServletResponse) {
    
    val contentType = request.getHeader("x-c3-type")

    response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
    getResultWriter(contentType).writeResponse(new ErrorResult(new ErrorDescription("Method not allowed")), response)
  }

  @ExceptionHandler(Array(classOf[ResourceNotFoundException]))
  def handleResourceNotFoundException(e:ResourceNotFoundException,
                                      request:HttpServletRequest,
                                      response:HttpServletResponse) {

    val contentType = request.getHeader("x-c3-type")

    response.setStatus(HttpServletResponse.SC_NOT_FOUND)
    getResultWriter(contentType).writeResponse(new ErrorResult(new ErrorDescription("Resource not found")), response)
  }

  @ExceptionHandler(Array(classOf[AuthFailedException]))
  def handleAuthFailedException(e:AuthFailedException,
                                      request:HttpServletRequest,
                                      response:HttpServletResponse) {

    val contentType = request.getHeader("x-c3-type")

    response.setStatus(HttpServletResponse.SC_FORBIDDEN)
    getResultWriter(contentType).writeResponse(new ErrorResult(new ErrorDescription("Authentication failed")), response)
  }

  @ExceptionHandler(Array(classOf[DomainException]))
  def handleDomainException(e:DomainException,
                                      request:HttpServletRequest,
                                      response:HttpServletResponse) {

    val contentType = request.getHeader("x-c3-type")

    response.setStatus(HttpServletResponse.SC_FORBIDDEN)
    getResultWriter(contentType).writeResponse(new ErrorResult(new ErrorDescription(e.getMessage)), response)
  }

  @ExceptionHandler(Array(classOf[FSNotFoundException]))
  def handleDomainException(e:FSNotFoundException,
                                      request:HttpServletRequest,
                                      response:HttpServletResponse) {

    val contentType = request.getHeader("x-c3-type")

    response.setStatus(HttpServletResponse.SC_NOT_FOUND)
    getResultWriter(contentType).writeResponse(new ErrorResult(new ErrorDescription("File not found")), response)
  }

  @ExceptionHandler(Array(classOf[FSWrongRequestException]))
  def handleDomainException(e:FSWrongRequestException,
                                      request:HttpServletRequest,
                                      response:HttpServletResponse) {

    val contentType = request.getHeader("x-c3-type")

    response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
    getResultWriter(contentType).writeResponse(new ErrorResult(new ErrorDescription(e.getMessage)), response)
  }

  protected def getResultWriter(expectedType:String):ResultWriter = {
    writerSelector.selectWriterForType(expectedType)
  }
}