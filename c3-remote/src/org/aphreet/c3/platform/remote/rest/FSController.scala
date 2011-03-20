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

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.aphreet.c3.platform.filesystem.{Directory, FSManager}
import response.fs.FSDirectory
import org.springframework.web.bind.annotation.{RequestParam, RequestHeader, RequestMethod, RequestMapping}
import org.aphreet.c3.platform.resource.Resource
import response.{Result, DirectoryResult}
import org.aphreet.c3.platform.domain.Domain

@Controller
@RequestMapping(Array("/fs/**"))
class FSController extends DataController{

  val baseUrl = "/rest/fs"

  var filesystemManager:FSManager = _

  @Autowired
  def setFilesystemManager(manager:FSManager) = {filesystemManager = manager}

  @RequestMapping(method = Array(RequestMethod.GET))
  def getNode(@RequestHeader(value = "x-c3-type", required = false) contentType:String,
              @RequestParam(value = "metadata", required = false) metadata:String,
              request:HttpServletRequest,
              response:HttpServletResponse){

    val domain = getRequestDomain(request, true)

    val fsPath = getFilesystemPath(request)

    val node = filesystemManager.getNode(domain, fsPath)

    if(metadata == null){

      if(node.isDirectory){
        sendDirectoryContents(node, contentType, domain, response)
      }else{
        sendResourceData(node.resource, -1, domain, response)
      }

    }else{
      sendMetadata(node.resource, contentType, domain, response)
    }
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  def makeNode(@RequestHeader(value = "x-c3-type", required = false) contentType:String,
               @RequestHeader(value = "x-c3-nodetype", required = false) nodetype:String,
               request:HttpServletRequest,
               response:HttpServletResponse){

    val domain = getRequestDomain(request, false)

    val fsPath = getFilesystemPath(request)

    if(nodetype == "directory"){

      filesystemManager.createDirectory(domain, fsPath)

      reportSuccess(HttpServletResponse.SC_CREATED, contentType, response)
    }else{

      val resource = new Resource

      executeDataUpload(resource, domain, request, response, () => {
        resource.systemMetadata.put(Domain.MD_FIELD, domain)

        filesystemManager.createFile(domain, fsPath, resource)

        reportSuccess(HttpServletResponse.SC_CREATED, contentType, response)
      })
    }
  }

  @RequestMapping(method = Array(RequestMethod.PUT))
  def updateNode(@RequestHeader(value = "x-c3-type", required = false) contentType:String,
                 request:HttpServletRequest,
                 response:HttpServletResponse){

    val domain = getRequestDomain(request, false)

    val fsPath = getFilesystemPath(request)

    val node = filesystemManager.getNode(domain, fsPath)

    if(node.isDirectory){
      throw new WrongRequestException("Can't update directory")
    }

    val resource = node.resource

    checkDomainAccess(node.resource, domain)    

    executeDataUpload(resource, domain, request, response, () => {
      val ra = accessManager.update(resource)
      reportSuccess(HttpServletResponse.SC_OK, contentType, response)
    })

  }

  @RequestMapping(method = Array(RequestMethod.DELETE))
  def deleteNode(@RequestHeader(value = "x-c3-type", required = false) contentType:String,
                 request:HttpServletRequest,
                 response:HttpServletResponse){

    val domain = getRequestDomain(request, false)

    val fsPath = getFilesystemPath(request)

    filesystemManager.deleteNode(domain, fsPath)

    reportSuccess(HttpServletResponse.SC_OK, contentType, response)
  }

  private def reportSuccess(code:Int, contentType:String, response:HttpServletResponse){
    response.setStatus(code)
    getResultWriter(contentType).writeResponse(new Result, response)
  }

  private def getFilesystemPath(request:HttpServletRequest):String = {
    request.getRequestURI.replaceFirst(baseUrl, "")
  }
}