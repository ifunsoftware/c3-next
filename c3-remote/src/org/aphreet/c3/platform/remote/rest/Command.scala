package org.aphreet.c3.platform.remote.rest

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.aphreet.c3.platform.access.PlatformAccessEndpoint
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.web.context.ContextLoader
import org.springframework.web.context.support.SpringBeanAutowiringSupport
import org.springframework.beans.factory.annotation.Autowired

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 6, 2010
 * Time: 12:50:19 AM
 * To change this template use File | Settings | File Templates.
 */

abstract class Command(val req:HttpServletRequest, val resp:HttpServletResponse) extends SpringBeanAutowiringSupport {

  var accessEndpoint:PlatformAccessEndpoint = null

  @Autowired
  def setPlatformAccessEndpoint(endpoint:PlatformAccessEndpoint) = {
    accessEndpoint = endpoint
  }

  def execute

  protected def parseURI:(String, Int) = {

    val parts = req.getRequestURI.split("/+");

    parts.length match {
      case 2 => (null, -1)
      case 3 => (parts(2), -1)
      case 4 => {
        try{
          (parts(2), Integer.parseInt(parts(3)))
        }catch{
          case e => throw new URIParseException
        }
      }
      case _ => throw new URIParseException
    }

  }

  protected def badRequest = resp.setStatus(HttpServletResponse.SC_BAD_REQUEST)

  protected def notFound = resp.setStatus(HttpServletResponse.SC_NOT_FOUND)
}

class URIParseException extends Exception