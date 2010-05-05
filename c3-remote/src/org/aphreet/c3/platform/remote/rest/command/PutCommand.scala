package org.aphreet.c3.platform.remote.rest.command

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.servlet.ServletContext
import org.aphreet.c3.platform.resource.Resource

/**
 * Created by IntelliJ IDEA.
 * User: malygm
 * Date: May 5, 2010
 * Time: 4:58:33 PM
 * To change this template use File | Settings | File Templates.
 */

class PutCommand(override val req:HttpServletRequest,
                  override val resp:HttpServletResponse,
                  override val context:ServletContext) extends UploadCommand(req, resp, context){
  

  override def getResource:Resource = {
    val req = parseURI

    if(req._1 != null) accessEndpoint.get(req._1)
    else throw new WrongRequestException
  }

  def processUpload(resource:Resource) = {
     val ra = accessEndpoint.update(resource)
     resp.getWriter.println(ra + "@" + resource.versions.length)
  }

}