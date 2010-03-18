package org.aphreet.c3.platform.remote.ws

import org.springframework.stereotype.Component
import com.sun.net.httpserver.{HttpPrincipal, Authenticator, HttpExchange, BasicAuthenticator}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 22, 2010
 * Time: 2:01:29 AM
 * To change this template use File | Settings | File Templates.
 */

@Component
class PlatformWSAuthenticator extends BasicAuthenticator("C3WS") {

  override def authenticate(exchange:HttpExchange):Authenticator.Result = {
    val uri:String = exchange.getRequestURI.toString

    if(uri.matches("/[A-Za-z]+\\?WSDL") && exchange.getRequestMethod.toLowerCase == "get"){
      new Authenticator.Success(new HttpPrincipal("wsdl-reader","ok"))  
    }else super.authenticate(exchange)
  }

  override def checkCredentials(username:String, password:String):Boolean = {
    val result = username == "admin" && password == "password"
    result
  }
}
