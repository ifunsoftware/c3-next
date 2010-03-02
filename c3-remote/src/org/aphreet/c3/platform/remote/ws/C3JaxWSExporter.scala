package org.aphreet.c3.platform.remote.ws

import org.springframework.remoting.jaxws.SimpleHttpServerJaxWsServiceExporter
import javax.xml.ws.Endpoint
import javax.jws.WebService

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 3, 2010
 * Time: 12:19:36 AM
 * To change this template use File | Settings | File Templates.
 */

class C3JaxWSExporter extends  SimpleHttpServerJaxWsServiceExporter {

  override protected def publishEndpoint(endpoint:Endpoint, annotation:WebService){
    try{
      super.publishEndpoint(endpoint, annotation)
    }catch{
      case e =>{
        this.destroy
        throw e
      }
    }
  }
}