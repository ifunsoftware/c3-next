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
import response.{DirectoryResult, Result}
import org.springframework.web.bind.annotation.{RequestParam, RequestHeader, RequestMethod, RequestMapping}

@Controller
@RequestMapping(Array("/fs"))
class FSController extends AbstractController{

  val baseUrl = "/rest/fs"

  var filesystemManager:FSManager = _

  @Autowired
  def setFilesystemManager(manager:FSManager) = {filesystemManager = manager}

  @RequestMapping(method = Array(RequestMethod.GET))
  def getNode(@RequestHeader(value = "x-c3-type", required = false) contentType:String,
              @RequestHeader(value = "x-c3-auth", required = false) authHeader:String,
              request:HttpServletRequest,
              response:HttpServletResponse){
    
    val fsPath = request.getRequestURI.replaceFirst(baseUrl, "")

    val node = filesystemManager.getNode(fsPath)

    if(node.isDirectory){
      getResultWriter(contentType).writeResponse(new DirectoryResult(FSDirectory.fromNode(node.asInstanceOf[Directory])), response)
    }else{
      sendResourceData(node.resource, -1, getCurrentUser(authHeader, request.getRequestURI), response)
    }
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  def makeDir(@RequestHeader(value = "x-c3-auth", required = false) authHeader:String,
              @RequestParam(value = "directory", required = false) directory:String,
              request:HttpServletRequest,
              response:HttpServletResponse){

    val fsPath = request.getRequestURI.replaceFirst(baseUrl, "")

    filesystemManager.createDirectory(fsPath)

    response.setStatus(HttpServletResponse.SC_CREATED)

  }
}