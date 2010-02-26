package org.aphreet.c3.platform.remote.ws

import org.springframework.remoting.jaxws.SimpleHttpServerJaxWsServiceExporter
import javax.xml.ws.Endpoint
import javax.jws.WebService

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 27, 2010
 * Time: 1:44:21 AM
 * To change this template use File | Settings | File Templates.
 */

class C3HttpServletJaxWsServiceExporter extends SimpleHttpServerJaxWsServiceExporter{

  override def publishEndpoint(endpoint:Endpoint, annotation:WebService) = {

    try{
      super.publishEndpoint(endpoint, annotation)
    }catch{
      case e => e.printStackTrace
      throw e
    }
  }
}