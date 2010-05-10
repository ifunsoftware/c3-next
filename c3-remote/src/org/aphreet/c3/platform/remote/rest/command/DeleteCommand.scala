package org.aphreet.c3.platform.remote.rest.command

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.aphreet.c3.platform.exception.{StorageIsNotWritableException, ResourceNotFoundException}
import org.aphreet.c3.platform.remote.rest.{ResourceRequest, Command}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 8, 2010
 * Time: 9:56:24 PM
 * To change this template use File | Settings | File Templates.
 */

class DeleteCommand(override val req:HttpServletRequest, override val resp:HttpServletResponse)
  extends HttpCommand(req, resp){

  override def execute{
    try{

      if(query != null && requestType == ResourceRequest){
        accessEndpoint.delete(query)
        ok
      }else badRequest
      
    }catch{
      case e:ResourceNotFoundException => notFound
      case e:StorageIsNotWritableException => forbidden
    }
  }

}