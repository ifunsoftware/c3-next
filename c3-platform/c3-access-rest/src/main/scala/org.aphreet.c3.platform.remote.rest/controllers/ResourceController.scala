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
import org.springframework.web.bind.annotation._

import javax.servlet.http._
import org.aphreet.c3.platform.filesystem.Node
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.remote.rest.response.{Result, ResourceAddress, UploadResult}
import org.aphreet.c3.platform.accesscontrol.{DELETE, UPDATE, CREATE, READ}


@Controller
@RequestMapping(Array("/resource"))
class ResourceController extends DataController {

  @RequestMapping(value = Array("/{address}"),
    method = Array(RequestMethod.GET))
  def getResource(@PathVariable address: String,
                  @RequestParam(value = "metadata", required = false) metadata: String,
                  @RequestHeader(value = "x-c3-extmeta", required = false) extMeta: String,
                  @RequestHeader(value = "x-c3-meta", required = false) childMeta:String,
                  @RequestHeader(value = "x-c3-type", required = false) contentType: String,
                  @RequestHeader(value = "x-c3-data", required = false) childData:String,
                  request: HttpServletRequest,
                  response: HttpServletResponse) {

    val accessTokens = getAccessTokens(READ, request)

    val resource = accessManager.get(address)

    if (metadata != null) {
      addNonPersistentMetadata(resource, extMeta)
      sendMetadata(resource, contentType, accessTokens, response)
    } else {

      val directoryNode: Node =
        if (Node.canBuildFromResource(resource)) {
          val node = Node.fromResource(resource)
          if (node.isDirectory) node
          else null
        } else null

      if (directoryNode != null) {
        sendDirectoryContents(directoryNode, childMeta, (childData != null), contentType, accessTokens, response)
      } else {
        sendResourceData(resource, -1, accessTokens, response)
      }
    }
  }

  @RequestMapping(value = Array("/{address}/{version}"),
    method = Array(RequestMethod.GET))
  def getResourceVersion(@PathVariable("address") address: String,
                         @PathVariable("version") version: Int,
                         @RequestParam(value = "metadata", required = false) metadata: String,
                         @RequestHeader(value = "x-c3-type", required = false) contentType: String,
                         @RequestHeader(value = "x-c3-extmeta", required = false) extMeta: String,
                         request: HttpServletRequest,
                         response: HttpServletResponse) {

    val accessTokens = getAccessTokens(READ, request)

    val resource = accessManager.get(address)

    if (metadata != null) {

      addNonPersistentMetadata(resource, extMeta)
      sendMetadata(resource, contentType, accessTokens, response)

    } else {
      sendResourceData(resource, version, accessTokens, response)
    }
  }


  @RequestMapping(method = Array(RequestMethod.POST))
  def saveResource(@RequestHeader(value = "x-c3-type", required = false) contentType: String,
                   request: HttpServletRequest,
                   response: HttpServletResponse) {

    val accessTokens = getAccessTokens(CREATE, request)

    val resource = new Resource

    executeDataUpload(resource, accessTokens, request, response, () => {

      val ra = accessManager.add(resource)
      response.setStatus(HttpServletResponse.SC_CREATED)
      getResultWriter(contentType).writeResponse(new UploadResult(new ResourceAddress(ra, 1)), response)

    })

  }

  @RequestMapping(value = Array("/{address}"), method = Array(RequestMethod.PUT))
  def updateResource(@PathVariable address: String,
                     @RequestHeader(value = "x-c3-type", required = false) contentType: String,
                     request: HttpServletRequest,
                     response: HttpServletResponse) {

    val accessTokens = getAccessTokens(UPDATE, request)

    val resource = accessManager.get(address)

    accessTokens.checkAccess(resource)

    executeDataUpload(resource, accessTokens, request, response, () => {
      val ra = accessManager.update(resource)
      getResultWriter(contentType)
        .writeResponse(new UploadResult(new ResourceAddress(ra, resource.versions.length)), response)
    })

  }


  @RequestMapping(value = Array("/{address}"), method = Array(RequestMethod.DELETE))
  def deleteResource(@PathVariable address: String,
                     @RequestHeader(value = "x-c3-type", required = false) contentType: String,
                     request: HttpServletRequest,
                     response: HttpServletResponse) {

    val accessTokens = getAccessTokens(DELETE, request)

    val resource = accessManager.get(address)

    accessTokens.checkAccess(resource)

    accessManager.delete(address)

    getResultWriter(contentType).writeResponse(new Result, response)
  }
}