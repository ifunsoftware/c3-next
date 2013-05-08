/*
 * Copyright (c) 2012, Mikhail Malygin
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
 * 3. Neither the name of the iFunSoftware nor the names of its contributors
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
package org.aphreet.c3.platform.remote.rest.controllers

import org.springframework.stereotype.Controller
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.web.bind.annotation.{RequestParam, RequestHeader, RequestMethod, RequestMapping}
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.remote.rest.response.Result
import org.apache.commons.httpclient.util.URIUtil
import java.io.BufferedReader
import org.aphreet.c3.platform.accesscontrol._

@Controller
@RequestMapping(Array("/fs/**"))
class FSController extends DataController {

  val baseUrl = "/rest/fs"

  @RequestMapping(method = Array(RequestMethod.GET), produces = Array("application/json", "application/xml"))
  def getNode(@RequestHeader(value = "x-c3-extmeta", required = false) extMeta: String,
              @RequestHeader(value = "x-c3-meta", required = false) childMeta:String,
              @RequestHeader(value = "x-c3-data", required = false) childData:String,
              @RequestParam(value = "metadata", required = false) metadata: String,
              request: HttpServletRequest,
              response: HttpServletResponse) {

    val accessTokens = getAccessTokens(READ, request)

    val domain = getCurrentDomainId(accessTokens)

    val fsPath = getFilesystemPath(request)

    val node = filesystemManager.getNode(domain, fsPath)

    if (metadata == null) {

      if (node.isDirectory) {
        sendDirectoryContents(node, childMeta, (childData != null), accessTokens, request, response)
      } else {
        sendResourceData(node.resource, -1, accessTokens, response)
      }

    } else {
      addNonPersistentMetadata(node.resource, extMeta)
      sendMetadata(node.resource, accessTokens, request, response)
    }
  }

  @RequestMapping(method = Array(RequestMethod.POST), produces = Array("application/json", "application/xml"))
  def makeNode(@RequestHeader(value = "x-c3-nodetype", required = false) nodetype: String,
               request: HttpServletRequest,
               response: HttpServletResponse) {

    val accessTokens = getAccessTokens(CREATE, request)

    val domain = getCurrentDomainId(accessTokens)

    val fsPath = getFilesystemPath(request)

    if (nodetype == "directory") {

      val metadata = getMetadata(request)

      filesystemManager.createDirectory(domain, fsPath, metadata.toMap)

      reportSuccess(HttpServletResponse.SC_CREATED, request, response)
    } else {

      val resource = new Resource

      executeDataUpload(resource, accessTokens, request, response, () => {

        filesystemManager.createFile(domain, fsPath, resource)

        reportSuccess(HttpServletResponse.SC_CREATED, request, response)
      })
    }
  }

  @RequestMapping(method = Array(RequestMethod.PUT), produces = Array("application/json", "application/xml"))
  def updateNode(@RequestHeader(value = "x-c3-op", required = false) operation: String,
                 request: HttpServletRequest,
                 response: HttpServletResponse) {

    val accessTokens = getAccessTokens(UPDATE, request)
    val domain = getCurrentDomainId(accessTokens)

    val fsPath = getFilesystemPath(request)

    val node = filesystemManager.getNode(domain, fsPath)

    val resource = node.resource

    accessTokens.checkAccess(resource)

    if (operation != null && operation == "move") {

      val bufferedReader = new BufferedReader(request.getReader)

      val newPath = decodeFSPath(bufferedReader.readLine())

      filesystemManager.moveNode(domain, fsPath, newPath)

      reportSuccess(HttpServletResponse.SC_OK, request, response)

    } else {
      executeDataUpload(resource, accessTokens, request, response, () => {
        accessManager.update(resource)
        reportSuccess(HttpServletResponse.SC_OK, request, response)
      })
    }
  }

  @RequestMapping(method = Array(RequestMethod.DELETE), produces = Array("application/json", "application/xml"))
  def deleteNode(request: HttpServletRequest,
                 response: HttpServletResponse) {

    val accessTokens = getAccessTokens(DELETE, request)
    val domain = getCurrentDomainId(accessTokens)

    val fsPath = getFilesystemPath(request)

    filesystemManager.deleteNode(domain, fsPath)

    reportSuccess(HttpServletResponse.SC_OK, request, response)
  }

  private def reportSuccess(code: Int, request: HttpServletRequest, response: HttpServletResponse) {
    response.setStatus(code)
    getResultWriter(request).writeResponse(new Result, response)
  }

  private def getFilesystemPath(request: HttpServletRequest): String = {
    val path = request.getRequestURI.replaceFirst(baseUrl, "")
    decodeFSPath(path)
  }

  def decodeFSPath(path: String): String = {
    URIUtil.decode(path, "UTF-8")
  }
}