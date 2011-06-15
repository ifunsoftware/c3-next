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

import collection.mutable.HashMap
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMethod, RequestMapping}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.aphreet.c3.platform.query.QueryManager
import org.springframework.beans.factory.annotation.Autowired
import query.RestQueryConsumer
import org.aphreet.c3.platform.domain.Domain

@Controller
class QueryController extends DataController{

  var queryManager:QueryManager = _

  @Autowired
  def setQueryManager(manager:QueryManager) = {queryManager = manager}

  @RequestMapping(value =  Array("/query"),
                  method = Array(RequestMethod.GET))
  def executeQuery(req:HttpServletRequest, resp:HttpServletResponse){

    val domain = getRequestDomain(req, true)

    val map = new HashMap[String, String]

    val enum = req.getParameterNames

    while (enum.hasMoreElements) {
      val key: String = enum.nextElement.asInstanceOf[String]
      val value: String = req.getParameter(key)
      map.put(key, value)
    }

    val consumer = new RestQueryConsumer(resp.getWriter)

    queryManager.executeQuery(Map[String, String]() ++ map, Map(Domain.MD_FIELD -> domain), consumer)
    resp.flushBuffer

  }
}