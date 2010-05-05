package org.aphreet.c3.platform.remote.rest.command

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.aphreet.c3.platform.resource.Resource
import javax.servlet.ServletContext


/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 6, 2010
 * Time: 8:14:48 PM
 * To change this template use File | Settings | File Templates.
 */

class PostCommand(override val req:HttpServletRequest,
                  override val resp:HttpServletResponse,
                  override val context:ServletContext) extends UploadCommand(req, resp, context){

  override def getResource:Resource = new Resource

  def processUpload(resource:Resource) = {
     val ra = accessEndpoint.add(resource)
     resp.getWriter.println(ra)
  }

}