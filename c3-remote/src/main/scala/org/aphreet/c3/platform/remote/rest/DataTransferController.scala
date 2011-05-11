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
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.aphreet.c3.platform.resource.Resource
import java.io.BufferedOutputStream
import org.aphreet.c3.platform.exception.ResourceNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.access.AccessManager
import org.aphreet.c3.platform.remote.replication.ReplicationManager
import org.springframework.web.bind.annotation.{RequestHeader, RequestMethod, PathVariable, RequestMapping}
import org.aphreet.c3.platform.auth.impl.HashUtil

@Controller
@RequestMapping(Array("/data"))
class DataTransferController extends AbstractController{

  var accessManager:AccessManager = _

  var replicationManager:ReplicationManager = _

  @Autowired
  def setAccessManager(manager:AccessManager) = {
    accessManager = manager
  }

  @Autowired
  def setReplicationManager(manager:ReplicationManager) = {
    replicationManager = manager
  }

  @RequestMapping(value =  Array("/replication/{address}/{version}"),
                  method = Array(RequestMethod.GET))
  def getResourceDataVersion(@PathVariable("address") address:String,
                             @PathVariable("version") version:Int,
                             @RequestHeader(value = "x-c3-repl", required = false) signature:String,
                             @RequestHeader(value = "x-c3-host", required = false) targetHostId:String,
                             @RequestHeader(value = "x-c3-date", required = false) date:String,
                             request:HttpServletRequest,
                             response:HttpServletResponse) =
  {

    val targetHost = replicationManager.getReplicationTarget(targetHostId)

    if(targetHost != null){
      val hashBase = request.getRequestURI + targetHostId + date

      if(HashUtil.hmac(targetHost.getKey, hashBase) == signature){
         sendResourceData(accessManager.get(address), version, response)
      }else{
        response.setStatus(HttpServletResponse.SC_FORBIDDEN)
      }
    }else{
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
    }
  }

  protected def sendResourceData(resource:Resource, versionNumber:Int, resp:HttpServletResponse) = {

    val version =
      if(versionNumber == -1) resource.versions.size
      else versionNumber

    if(version > 0 && resource.versions.size >= version){

      val resourceVersion = resource.versions(version - 1)

      resp.reset
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.setContentLength(resourceVersion.data.length.toInt)

      resource.metadata.get(Resource.MD_CONTENT_TYPE) match {
        case Some(x) => resp.setContentType(x)
        case None =>
      }

      val os = new BufferedOutputStream(resp.getOutputStream)

      try {
        resourceVersion.data.writeTo(os)
      } finally {
        os.close
        resp.flushBuffer
      }

    }else{
      throw new ResourceNotFoundException("Incorrect version number")
    }
  }
}