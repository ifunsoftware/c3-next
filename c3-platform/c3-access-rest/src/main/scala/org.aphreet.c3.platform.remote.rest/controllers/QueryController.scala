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
import org.springframework.web.bind.annotation.{RequestHeader, RequestMethod, RequestMapping}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.aphreet.c3.platform.query.QueryManager
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.remote.rest.query.RestQueryConsumer
import org.aphreet.c3.platform.accesscontrol.READ
import collection.mutable
import org.aphreet.c3.platform.remote.rest.response.JsonResultWriter
import org.aphreet.c3.platform.remote.rest.response.XmlResultWriter


@Controller
class QueryController extends DataController {

  @Autowired
  var queryManager: QueryManager = _

  private val SYSTEM_META = "system."

  @RequestMapping(value = Array("/query"),
    method = Array(RequestMethod.GET))
  def executeQuery(req: HttpServletRequest,
                   resp: HttpServletResponse,
                   @RequestHeader(value = "x-c3-type", required = false) contentType: String) {
    val accessTokens = getAccessTokens(READ, req)

    val userMetaMap = new mutable.HashMap[String, String]
    val systemMetaMap = new mutable.HashMap[String, String]

    val enum = req.getParameterNames

    while (enum.hasMoreElements) {
      val key: String = enum.nextElement.asInstanceOf[String]
      val value: String = req.getParameter(key)
      if (key.startsWith(SYSTEM_META))
        systemMetaMap.put(key.replace(SYSTEM_META, ""), value)
      else
        userMetaMap.put(key, value)
    }

    val writer = resp.getWriter()
    val resultWriter = getResultWriter(contentType)

    val consumer = new RestQueryConsumer(writer, resultWriter)

    val (start, end) = resultWriter match {
      case jsonWrtr: JsonResultWriter => ("[", "]")
      case xmlWrtr: XmlResultWriter => ("<resources>", "</resources>")
      case _ => ("", "") // unknown result writer
    }

    writer.write(start)

    queryManager.executeQuery(userMetaMap.toMap, accessTokens.metadataRestrictions ++ systemMetaMap.toMap, consumer)

    writer.write(end)

    resp.flushBuffer()

  }
}