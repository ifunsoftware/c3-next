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

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.servlet.ServletContext
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.exception.ResourceNotFoundException
import org.aphreet.c3.platform.auth.exception.AuthFailedException

class PutCommand(override val req:HttpServletRequest,
                  override val resp:HttpServletResponse,
                  override val context:ServletContext) extends UploadCommand(req, resp, context){
  

  override def getResource:Resource = {

    if(query == null) throw new WrongRequestException


    val resource = accessManager.get(query)

    resource.systemMetadata.get(Resource.MD_USER) match{
      case Some(u) =>
        if(u != currentUser)
          throw new AuthFailedException
        else
          resource
      case None => resource
    }

  }

  def processUpload(resource:Resource) = {
     val ra = accessManager.update(resource)
     resp.getWriter.println(ra + "@" + resource.versions.length)
  }

}