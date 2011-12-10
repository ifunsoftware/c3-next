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
package org.aphreet.c3.platform.remote.ws

import javax.servlet._
import http.{HttpServletResponse, HttpServletRequest}
import org.aphreet.c3.platform.auth.AuthenticationManager
import com.sun.org.apache.xml.internal.security.utils.Base64
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.logging.LogFactory
import org.springframework.web.context.support.{SpringBeanAutowiringSupport}


class WSAuthFilter extends Filter{

  val log = LogFactory getLog getClass

  var authManager:AuthenticationManager = null

  @Autowired
  def setAuthManager(manager:AuthenticationManager) {
    authManager = manager
    log info "AuthManager setter invoked"
  }

  var filterConfig:FilterConfig = _


  override def init(config:FilterConfig){
    filterConfig = config
  }

  override def destroy(){}

  override def doFilter(request: ServletRequest, response: ServletResponse, chain:FilterChain) {

    if(authManager == null){
      log info "Processing Autowired.."
      SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this)
    }

    var authOk = false

    val req = request.asInstanceOf[HttpServletRequest]

    val auth = req.getHeader("Authorization")

    if(auth == null){

      if(req.getMethod == "GET" &&
              (req.getParameter("WSDL") != null || req.getParameter("wsdl") != null)){
        authOk = true
      }

    }else{
      try{
        val usernamePass = new String(Base64.decode(auth.replaceFirst("Basic\\s+", "")), "UTF-8")

        val array = usernamePass.split(":", 2)

        if(array.length == 2){
          val user = array(0)
          val password = array(1)

          authOk = authManager.auth(user, password) != null
        }
      }catch{
        case e => e.printStackTrace()
      }
    }

    if(authOk){
      chain.doFilter(request, response)
    }else{
      response.asInstanceOf[HttpServletResponse].setStatus(HttpServletResponse.SC_UNAUTHORIZED)
      response.asInstanceOf[HttpServletResponse].setHeader("WWW-Authenticate", "Basic realm=\"Secure Area\"")
    }
  }

}