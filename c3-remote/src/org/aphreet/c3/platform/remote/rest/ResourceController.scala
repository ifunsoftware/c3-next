/**
 * Copyright (c) 2010, Mikhail Malygin
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

import org.aphreet.c3.platform.resource._
import org.aphreet.c3.platform.remote.rest.response._

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._

import javax.servlet.http._
import org.aphreet.c3.platform.domain.Domain
import org.aphreet.c3.platform.filesystem.Node

@Controller
@RequestMapping(Array("/resource"))
class ResourceController extends DataController{

  @RequestMapping(value =  Array("/{address}"),
                  method = Array(RequestMethod.GET))
  def getResourceData(@PathVariable address:String,
                      @RequestParam(value = "metadata", required = false) metadata:String,
                      @RequestHeader(value = "x-c3-type", required = false) contentType:String,
                      request:HttpServletRequest,
                      response:HttpServletResponse) =
  {

    val domain = getRequestDomain(request, true)

    val resource = accessManager.get(address)

    if(metadata != null){
      sendMetadata(resource, contentType, domain, response)
    }else{
      sendResourceData(resource, -1, domain, response)
    }
  }

  @RequestMapping(value =  Array("/{address}/{version}"),
                  method = Array(RequestMethod.GET))
  def getResourceDataVersion(@PathVariable("address") address:String,
                             @PathVariable("version") version:Int,
                             @RequestParam(value = "metadata", required = false) metadata:String,
                             @RequestHeader(value = "x-c3-type", required = false) contentType:String,
                             request:HttpServletRequest,
                             response:HttpServletResponse) =
  {

    val domain = getRequestDomain(request, true)

    val resource = accessManager.get(address)

    if(metadata != null){
      sendMetadata(resource, contentType, domain, response)
    }else{
      sendResourceData(resource, version, domain, response)
    }
  }



  @RequestMapping(method = Array(RequestMethod.POST))
  def saveResource(@RequestHeader(value = "x-c3-type", required = false) contentType:String,
                   request:HttpServletRequest,
                   response:HttpServletResponse) =
  {

    val domain = getRequestDomain(request, false)

    val resource = new Resource

    executeDataUpload(resource, domain, request, response, () => {
      resource.systemMetadata.put(Domain.MD_FIELD, domain)
      val ra = accessManager.add(resource)
      response.setStatus(HttpServletResponse.SC_CREATED)
      getResultWriter(contentType).writeResponse(new UploadResult(new ResourceAddress(ra, 1)), response)
    })

  }

  @RequestMapping(value=Array("/{address}"), method = Array(RequestMethod.PUT))
  def updateResource(@PathVariable address:String,
                     @RequestHeader(value = "x-c3-type", required = false) contentType:String,
                     request:HttpServletRequest,
                     response:HttpServletResponse) =

  {
    val domain = getRequestDomain(request, false)

    val resource = accessManager.get(address)

    canEditResource(resource)
    checkDomainAccess(resource, domain)

    executeDataUpload(resource, domain, request, response, () => {
      val ra = accessManager.update(resource)
      getResultWriter(contentType).writeResponse(new UploadResult(new ResourceAddress(ra, resource.versions.length)), response)
    })

  }


  @RequestMapping(value=Array("/{address}"), method = Array(RequestMethod.DELETE))
  def deleteResource(@PathVariable address:String,
                     @RequestHeader(value = "x-c3-type", required = false) contentType:String,
                     request:HttpServletRequest,
                     response:HttpServletResponse) = {

    val domain = getRequestDomain(request, false)

    val resource = accessManager.get(address)

    canEditResource(resource)
    checkDomainAccess(resource, domain)

    accessManager.delete(address)
    
    getResultWriter(contentType).writeResponse(new Result, response)
  }

  def canEditResource(resource:Resource):Boolean = {
    resource.systemMetadata.get(Node.NODE_TYPE_FIELD) match {
      case Some(x) => x != Node.NODE_TYPE_FIELD
      case None => true
    }
  }

}