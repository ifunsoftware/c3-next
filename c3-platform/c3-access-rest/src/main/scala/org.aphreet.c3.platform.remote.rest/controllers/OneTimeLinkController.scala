/*
 * Copyright (c) 2013, Mikhail Malygin
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

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.aphreet.c3.platform.accesscontrol.{AccessTokens, READ}
import org.aphreet.c3.platform.auth.HashUtil
import org.aphreet.c3.platform.exception.ResourceNotFoundException
import org.aphreet.c3.platform.remote.rest.response.TempLinkResult
import org.aphreet.c3.platform.resource.Resource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMethod, RequestMapping}
import scala.Array
import org.aphreet.c3.platform.filesystem.Node

@Controller
@RequestMapping(Array("/once"))
class OneTimeLinkController extends DataController{

  var temporaryLinks: Map[String, LinkInfo] = Map()

  @RequestMapping(method = Array(RequestMethod.POST),
    value = Array("/{address}/{version}"),
    produces = Array("application/json", "application/xml"))
  def createLinkForResource(@PathVariable address: String,
                            @PathVariable version: Int,
                            request: HttpServletRequest,
                            response: HttpServletResponse){

    createTemporaryLink(address, version, request, response)
  }

  @RequestMapping(method = Array(RequestMethod.POST),
    value = Array("/{address}"),
    produces = Array("application/json", "application/xml"))
  def createLinkForLastResourceVersion(@PathVariable address: String,
                                       request: HttpServletRequest,
                                       response: HttpServletResponse){

    createTemporaryLink(address, -1, request, response)
  }

  def createTemporaryLink(address: String,
                          version: Int,
                          request: HttpServletRequest,
                          response: HttpServletResponse){
    val accessTokens = getAccessTokens(READ, request)

    val resource = accessManager.get(address)

    if(Node.canBuildFromResource(resource) && Node.fromResource(resource).isDirectory){
      throw new ResourceNotFoundException("Data not is not found");
    }

    accessTokens.checkAccess(resource)

    if(resource.versions.length < version){
      throw new ResourceNotFoundException("Incorrect version number")
    }

    val time = System.currentTimeMillis()

    val code = HashUtil.sha256hash(address + version + time)

    synchronized{
      temporaryLinks = temporaryLinks.filter{case (linkCode, info) => (time - info.time) < 60000}

      temporaryLinks += ((code, LinkInfo(address, version, System.currentTimeMillis())))
    }

    getResultWriter(request).writeResponse(new TempLinkResult("/once/" + code), response)
  }

  @RequestMapping(method = Array(RequestMethod.GET),
    value = Array("/{code}"))
  def downloadResourceBlob(@PathVariable code: String,
                           request: HttpServletRequest,
                           response: HttpServletResponse){

    temporaryLinks.get(code) match {
      case Some(link) => {
          val resource = accessManager.get(link.address)
          sendResourceData(resource, link.version, NullAccessToken, response)
      }
      case None => throw new ResourceNotFoundException("Incorrect access code")
    }
  }
}

case class LinkInfo(address: String, version: Int, time: Long)

object NullAccessToken extends AccessTokens{
  def checkAccess(resource: Resource) {}

  def updateMetadata(resource: Resource) {}

  def tokenForName(name: String) = None

  def metadataRestrictions = Map()
}