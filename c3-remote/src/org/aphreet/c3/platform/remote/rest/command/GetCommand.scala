package org.aphreet.c3.platform.remote.rest.command

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.aphreet.c3.platform.resource.{Resource, ResourceVersion}
import org.aphreet.c3.platform.exception.ResourceNotFoundException
import org.aphreet.c3.platform.remote.rest._
import java.util.List

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 6, 2010
 * Time: 12:53:09 AM
 * To change this template use File | Settings | File Templates.
 */

class GetCommand(override val req:HttpServletRequest, override val resp:HttpServletResponse)
  extends HttpCommand(req, resp){

  override def execute{
    try{

      requestType match {
        case ResourceRequest => executeResource
        case SearchRequest => executeSearch
      }

    }catch{
      case e:URIParseException => badRequest
      case e:ResourceNotFoundException => notFound
    }
  }

  def executeResource = {
    
    if(query != null){

        val resource = accessEndpoint.get(query)

        if(resource != null){
          if(resourcePart == ResourceMetadata){
            sendResourceMetadata(resource)
          }else{
            var vers = 0
            if(version >= 0) vers = version

            if(resource.versions.size > vers){
              sendResource(resource, resource.versions(vers))
            }else notFound
          }


        }else notFound

      }else badRequest
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

  def sendResourceMetadata(resource:Resource){
    resp.reset
    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentType("text/x-json")
    resp.getWriter.write(resource.toJSON(false))
    resp.flushBuffer
  }

  def executeSearch = {
    if(query != null)
      sendSearchResults(accessEndpoint.search(query))
    else
      badRequest
  }

  def sendSearchResults(results:List[String]) = {
    resp.reset
    resp.setStatus(HttpServletResponse.SC_OK)
    resp.setContentType("text/x-json")

    val writer = resp.getWriter

    writer.write(
"""{
  resources:[
""")

    for(address <- results.toArray){
      writer.write("\"" + address.toString + "\",\n")
    }
    writer.write(
""" ]
}""")

    resp.flushBuffer
  }

}
