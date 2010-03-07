package org.aphreet.c3.platform.remote.rest

import command.{PostCommand, GetCommand}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 28, 2010
 * Time: 9:41:04 PM
 * To change this template use File | Settings | File Templates.
 */

class RestServlet extends HttpServlet {

  override def doGet(request:HttpServletRequest, response:HttpServletResponse) = {
    new GetCommand(request, response).execute
  }

  override def doPost(request:HttpServletRequest, response:HttpServletResponse) = {
    new PostCommand(request, response, getServletContext).execute
  }

  override def doPut(request:HttpServletRequest, response:HttpServletResponse) = {

  }

  override def doDelete(request:HttpServletRequest, response:HttpServletResponse) = {

  }
}

