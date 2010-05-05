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

  protected def parseURI:(String, ResourcePart, Int) = Command.parseURI(req.getRequestURI)

  protected def badRequest = resp.setStatus(HttpServletResponse.SC_BAD_REQUEST)

  protected def notFound = resp.setStatus(HttpServletResponse.SC_NOT_FOUND)

  protected def forbidden = resp.setStatus(HttpServletResponse.SC_FORBIDDEN)

  protected def ok = resp.setStatus(HttpServletResponse.SC_OK)


}

object Command{

 def parseURI(uri:String):(String, ResourcePart, Int) = {

    val parts = uri.split("/+");

    parts.length match {
      case 2 => (null, ResourceMetadata, -1)
      case 3 => (parts(2), ResourceMetadata, -1)
      case 4 => {
        try{
          (parts(2), partFromString(parts(3)), -1)
        }catch{
          case e => throw new URIParseException
        }
      }
      case 5 => {
        try{
          (parts(2), partFromString(parts(3)), Integer.parseInt(parts(4)))
        }
      }
      case _ => throw new URIParseException
    }

  }

  private def partFromString(part:String):ResourcePart = part match {
    case "data" => ResourceData
    case "metadata" => ResourceMetadata
    case _ => throw new URIParseException
  }

}

class URIParseException extends Exception

sealed class ResourcePart
object ResourceData extends ResourcePart
object ResourceMetadata extends ResourcePart