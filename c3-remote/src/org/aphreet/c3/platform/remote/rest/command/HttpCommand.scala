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
import org.aphreet.c3.platform.remote.rest.Command
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.auth.exception.AuthFailedException
import org.aphreet.c3.platform.auth.{ACCESS, AuthenticationManager}

class HttpCommand(val req:HttpServletRequest, val resp:HttpServletResponse)
        extends Command(req.getRequestURI, req.getContextPath){

  
  var authManager:AuthenticationManager = _

  lazy val currentUser:String = getCurrentUser

  @Autowired
  def setAuthenticationManager(manager:AuthenticationManager) = {
    authManager = manager
  }

  private def getCurrentUser:String = {

    val up = getUsernameAndPassword

    if(up != null){
      val user = authManager.authAccess(up._1, up._2, req.getRequestURI)

      if(user != null)
        return user.name

    }else{
      val anonymous = authManager.get("anonymous")

      if(anonymous != null && anonymous.enabled)
        return "anonymous"
    }

    throw new AuthFailedException
  }

  def getUsernameAndPassword:(String,String) = {


    val authHeader = req.getHeader(HttpCommand.AUTH_HEADER)

    if(authHeader != null){
      val array = authHeader.split(":", 2)
      if(array.length == 2)
        return (array(0), array(1))
    }

    null
  }


  protected def badRequest = resp.setStatus(HttpServletResponse.SC_BAD_REQUEST)

  protected def notFound = resp.setStatus(HttpServletResponse.SC_NOT_FOUND)

  protected def forbidden = resp.setStatus(HttpServletResponse.SC_FORBIDDEN)

  protected def ok = resp.setStatus(HttpServletResponse.SC_OK)

}

object HttpCommand{

  val AUTH_HEADER = "C3Auth"

}

object HttpConstants{

  val AUTH_HEADER = "C3Auth"
  
}