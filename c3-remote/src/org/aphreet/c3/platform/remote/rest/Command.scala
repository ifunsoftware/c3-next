package org.aphreet.c3.platform.remote.rest

import org.aphreet.c3.platform.access.PlatformAccessEndpoint
import org.springframework.web.context.support.SpringBeanAutowiringSupport
import org.springframework.beans.factory.annotation.Autowired

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 6, 2010
 * Time: 12:50:19 AM
 * To change this template use File | Settings | File Templates.
 */

class Command(val url:String, val contextPath:String) extends SpringBeanAutowiringSupport {

  var requestType:RequestType = null
  var query:String = null
  var version:Int = -1
  var resourcePart:ResourcePart = ResourceMetadata


  var accessEndpoint:PlatformAccessEndpoint = null

  @Autowired
  def setPlatformAccessEndpoint(endpoint:PlatformAccessEndpoint) = {
    accessEndpoint = endpoint
  }

  {
    val cleanUrl = url.replaceFirst(contextPath, "").replaceFirst("^/+", "").replaceFirst("/+$", "")

    //"resource/1231-1234-1234-1234/data/27


    val parts = cleanUrl.split("/+")

    if(parts.length > 0 ){
      requestType = RequestType.typeFromString(parts(0))

      if(parts.length > 1){
        query = parts(1)
      }

      if(parts.length > 2){
        resourcePart = ResourcePart.partFromString(parts(2))
      }

      if(parts.length > 3){
        try{
          version = Integer.parseInt(parts(3))
        }catch{
          case e:NumberFormatException => throw new URIParseException
        }
      }
    }else throw new URIParseException

  }


  def execute = {}


}

class URIParseException extends Exception

sealed class ResourcePart

object ResourcePart{

  def partFromString(part:String):ResourcePart = part match {
    case "data" => ResourceData
    case "metadata" => ResourceMetadata
    case _ => throw new URIParseException
  }

}

object ResourceData extends ResourcePart
object ResourceMetadata extends ResourcePart

sealed class RequestType
object RequestType{

  def typeFromString(str:String):RequestType = {
    str match {
      case "resource" => ResourceRequest
      case "search" => SearchRequest
      case _ => throw new URIParseException
    }

  }

}
object ResourceRequest extends RequestType
object SearchRequest extends RequestType
