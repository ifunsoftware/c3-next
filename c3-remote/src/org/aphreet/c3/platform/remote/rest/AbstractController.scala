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
import org.springframework.web.bind.annotation.{ResponseStatus, RequestHeader, ExceptionHandler}
import org.springframework.http.HttpStatus
import org.aphreet.c3.platform.auth.exception.AuthFailedException
import java.io.BufferedOutputStream
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.auth.AuthenticationManager

class AbstractController{

  var authManager:AuthenticationManager = _

  var writerSelector:ResultWriterSelector = _

  @Autowired
  def setResultWriterSelector(selector:ResultWriterSelector) = {
    writerSelector = selector
  }
  
  @Autowired
  def setAuthenticationManager(manager:AuthenticationManager) = {
    authManager = manager
  }

  @ExceptionHandler(Array(classOf[Exception]))
  def handleException(e:Exception,
                      request:HttpServletRequest,
                      response:HttpServletResponse) {

    val contentType = request.getHeader("x-c3-type")
    
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)

    getResultWriter(contentType).writeResponse(new ErrorResult(new ErrorDescription("Internal Server Error", e)), response)
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

  protected def getResultWriter(expectedType:String):ResultWriter = {
    writerSelector.selectWriterForType(expectedType)
  }

  def sendResourceData(resource:Resource, versionNumber:Int, username:String, resp:HttpServletResponse) = {

    val version =
      if(versionNumber == -1) resource.versions.size
      else versionNumber

    if(version > 0 && resource.versions.size >= version){

      val resourceVersion = resource.versions(version - 1)

      resp.reset
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.setContentLength(resourceVersion.data.length.toInt)

      resource.metadata.get(Resource.MD_CONTENT_TYPE) match {
        case Some(x) => resp.setContentType(x)
        case None =>
      }

      val os = new BufferedOutputStream(resp.getOutputStream)

      try {
        resourceVersion.data.writeTo(os)
      } finally {
        os.close
        resp.flushBuffer
      }

    }else{
      throw new ResourceNotFoundException("Incorrect version number")
    }
  }

  def getCurrentUser(authHeader:String, requestUri:String):String = {

    if(authHeader != null){
      val array = authHeader.split(":", 2)
      if(array.length == 2){

        val user = authManager.authAccess(array(0), array(1), requestUri)
        if(user != null){
          return user.name
        }else{
          throw new AuthFailedException("Incorrect key")
        }

      }else{
        throw new AuthFailedException("Incorrect header format")
      }
    }else{

      val anonymous = authManager.get("anonymous")

      if(anonymous != null && anonymous.enabled)
        return anonymous.name
      else
        throw new AuthFailedException("Anonymous is disabled")

    }
  }
}