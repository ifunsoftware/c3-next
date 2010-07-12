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

package org.aphreet.c3.platform.remote.rest.command

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.aphreet.c3.platform.resource.{Resource, ResourceVersion}
import org.aphreet.c3.platform.exception.ResourceNotFoundException
import org.aphreet.c3.platform.remote.rest._
import java.util.List
import query.ServletQueryConsumer
import java.io.BufferedOutputStream
import collection.mutable.HashMap
import org.aphreet.c3.platform.query.QueryManager
import org.springframework.beans.factory.annotation.Autowired

class GetCommand(override val req: HttpServletRequest, override val resp: HttpServletResponse)
        extends HttpCommand(req, resp) {
  var queryManager: QueryManager = _

  @Autowired
  def setQueryManager(manager: QueryManager) = {queryManager = manager}


  override def execute {
    try {

      requestType match {
        case ResourceRequest => executeResource
        case SearchRequest => executeSearch
        case QueryRequest => executeQuery
      }

    } catch {
      case e: URIParseException => badRequest
      case e: ResourceNotFoundException => notFound
    }
  }

  def executeResource = {

    if (query != null) {

      val resource = accessManager.get(query)

      if (resource != null) {
        if (resourcePart == ResourceMetadata) {
          sendResourceMetadata(resource)
        } else {
          var vers = 0
          if (version >= 0) vers = version

          if (resource.versions.size > vers) {
            sendResource(resource, resource.versions(vers))
          } else notFound
        }


      } else notFound

    } else badRequest
  }

  def sendResource(resource: Resource, version: ResourceVersion) {
    resp.reset
    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentLength(version.data.length.toInt)
    resp.setContentType(resource.getMetadata.get(Resource.MD_CONTENT_TYPE))

    val os = new BufferedOutputStream(resp.getOutputStream)
    try {
      version.data.writeTo(os)
    } finally {
      os.close
      resp.flushBuffer
    }

  }

  def sendResourceMetadata(resource: Resource) {
    //TODO remove this in release
    val str = resource.toJSON(req.getParameterMap.containsKey("system"))

    resp.reset
    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentType("text/plain")
    resp.setCharacterEncoding("UTF-8")

    val bytes = str.getBytes("UTF-8")

    resp.setContentLength(bytes.length)

    resp.getOutputStream.write(bytes)

    resp.flushBuffer
  }

  def executeSearch = {
    if (query != null)
    //sendSearchResults(accessEndpoint.search(query))
      notFound
    else
      badRequest
  }

  def executeQuery = {

    val map = new HashMap[String, String]

    val enum = req.getParameterNames

    while (enum.hasMoreElements) {
      val key: String = enum.nextElement.asInstanceOf[String]
      val value: String = req.getParameter(key)
      map.put(key, value)
    }

    val consumer = new ServletQueryConsumer(resp.getWriter, map)

    queryManager.executeQuery(consumer)
    resp.flushBuffer
  }

  def sendSearchResults(results: List[String]) = {
    resp.reset
    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentType("text/x-json")

    val writer = resp.getWriter

    writer.write(
      """{
        resources:[
      """)

    for (address <- results.toArray) {
      writer.write("\"" + address.toString + "\",\n")
    }
    writer.write(
      """ ]
      }""")

    resp.flushBuffer
  }

}
