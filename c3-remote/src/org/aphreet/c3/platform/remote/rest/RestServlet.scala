package org.aphreet.c3.platform.remote.rest

import command.{PutCommand, DeleteCommand, PostCommand, GetCommand}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 28, 2010
 * Time: 9:41:04 PM
 * To change this template use File | Settings | File Templates.
 */

class RestServlet extends HttpServlet {

  override def service(request:HttpServletRequest, response:HttpServletResponse) = {
    try{
      super.service(request, response)
    }catch {
      case e:URIParseException => response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
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

