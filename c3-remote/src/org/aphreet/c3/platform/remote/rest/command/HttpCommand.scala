package org.aphreet.c3.platform.remote.rest.command

import org.aphreet.c3.platform.access.PlatformAccessEndpoint
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.web.context.support.SpringBeanAutowiringSupport
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.remote.rest.{ResourcePart, Command}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: May 9, 2010
 * Time: 3:01:08 PM
 * To change this template use File | Settings | File Templates.
 */

class HttpCommand(val req:HttpServletRequest, val resp:HttpServletResponse)
        extends Command(req.getRequestURI, req.getContextPath){

  protected def badRequest = resp.setStatus(HttpServletResponse.SC_BAD_REQUEST)

  protected def notFound = resp.setStatus(HttpServletResponse.SC_NOT_FOUND)

  protected def forbidden = resp.setStatus(HttpServletResponse.SC_FORBIDDEN)

  protected def ok = resp.setStatus(HttpServletResponse.SC_OK)

}