package org.aphreet.c3.platform.remote.rest.command

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.aphreet.c3.platform.remote.rest.{URIParseException, Command}
import org.aphreet.c3.platform.resource.{Resource, ResourceVersion}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 6, 2010
 * Time: 12:53:09 AM
 * To change this template use File | Settings | File Templates.
 */

class GetCommand(override val req:HttpServletRequest, override val resp:HttpServletResponse)
  extends Command(req, resp){

  override def execute{
    try{
      val req = parseURI

      if(req._1 != null){

        val resource = accessEndpoint.get(req._1)

        if(resource != null){
          var vers = 0

          if(req._2 >= 0){
            vers = req._2
          }

          if(resource.versions.size > vers){
            sendResource(resource, resource.versions(vers))
          }else notFound
        }else notFound

      }else badRequest
    }catch{
      case e:URIParseException => badRequest
    }
  }

  def sendResource(resource:Resource, version:ResourceVersion){
    resp.reset
    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentLength(version.data.length.toInt)
    resp.setContentType(resource.getMetadata.get(Resource.MD_CONTENT_TYPE))

    val os = resp.getOutputStream
    try{
      version.data.writeTo(resp.getOutputStream)
    }finally {
      os.close
      resp.flushBuffer
    }

  }

}
