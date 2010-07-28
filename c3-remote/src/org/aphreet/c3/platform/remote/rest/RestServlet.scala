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

import command.{PutCommand, DeleteCommand, PostCommand, GetCommand}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}

class RestServlet extends HttpServlet {

  override def service(request:HttpServletRequest, response:HttpServletResponse) = {
    try{
      super.service(request, response)
    }catch {
      case e:URIParseException =>
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        response.getWriter.println(e.message)
    }
  }

  override def doGet(request:HttpServletRequest, response:HttpServletResponse) = {
    new GetCommand(request, response).execute
  }

  override def doPost(request:HttpServletRequest, response:HttpServletResponse) = {
    new PostCommand(request, response, getServletContext).execute
  }

  override def doPut(request:HttpServletRequest, response:HttpServletResponse) = {
    new PutCommand(request, response, getServletContext).execute
  }

  override def doDelete(request:HttpServletRequest, response:HttpServletResponse) = {
    new DeleteCommand(request, response).execute
  }
}

