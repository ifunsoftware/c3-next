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

import org.aphreet.c3.platform.auth._
import org.springframework.stereotype.Component
import com.sun.net.httpserver.{HttpPrincipal, Authenticator, HttpExchange, BasicAuthenticator}
import org.springframework.beans.factory.annotation.Autowired

@Component
class PlatformWSAuthenticator extends BasicAuthenticator("C3WS") {

  var authManager:AuthenticationManager = _

  @Autowired
  def setAuthManager(manager:AuthenticationManager) = {authManager = manager}

  override def authenticate(exchange:HttpExchange):Authenticator.Result = {
    val uri:String = exchange.getRequestURI.toString

    if(uri.matches("/[A-Za-z]+\\?WSDL") && exchange.getRequestMethod.toLowerCase == "get"){
      new Authenticator.Success(new HttpPrincipal("wsdl-reader","ok"))  
    }else super.authenticate(exchange)
  }

  override def checkCredentials(username:String, password:String):Boolean = {

    val user = authManager.authenticate(username, password, MANAGEMENT)
    
    user != null
  }
}
