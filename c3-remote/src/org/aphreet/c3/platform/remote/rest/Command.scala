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

import org.springframework.web.context.support.SpringBeanAutowiringSupport
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.access.{AccessManager}


class Command(val url:String, val contextPath:String) extends SpringBeanAutowiringSupport {

  var requestType:RequestType = null
  var query:String = null
  var version:Int = -1
  var resourcePart:ResourcePart = ResourceMetadata


  var accessManager:AccessManager = null

  @Autowired
  def setAccessManager(manager:AccessManager) = {
    accessManager = manager
  }

  {
    val cleanUrl = url.replaceFirst(contextPath, "").replaceFirst("^/+", "").replaceFirst("/+$", "")

    //"resource/1231-1234-1234-1234/data/27


    val parts = cleanUrl.split("/+")

    if(parts.length > 0 ){
      requestType = RequestType.typeFromString(parts(0))

      if(parts.length > 1){
        query = parts(1)
      }

      if(parts.length > 2){
        resourcePart = ResourcePart.partFromString(parts(2))
      }

      if(parts.length > 3){
        try{
          version = Integer.parseInt(parts(3))
        }catch{
          case e:NumberFormatException => throw new URIParseException("Incorrect version number")
        }
      }
    }else throw new URIParseException("Empty path")

  }


  def execute = {}


}

class URIParseException(val message:String) extends Exception(message)

sealed class ResourcePart

object ResourcePart{

  def partFromString(part:String):ResourcePart = part match {
    case "data" => ResourceData
    case "metadata" => ResourceMetadata
    case _ => throw new URIParseException("Incorrect resource part")
  }

}

object ResourceData extends ResourcePart
object ResourceMetadata extends ResourcePart

sealed class RequestType
object RequestType{

  def typeFromString(str:String):RequestType = {
    str match {
      case "resource" => ResourceRequest
      case "search" => SearchRequest
      case "query" => QueryRequest
      case _ => throw new URIParseException("Incorrect service name")
    }

  }

}
object ResourceRequest extends RequestType
object SearchRequest extends RequestType
object QueryRequest extends RequestType