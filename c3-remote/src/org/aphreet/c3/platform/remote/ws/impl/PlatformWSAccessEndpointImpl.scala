package org.aphreet.c3.platform.remote.api.ws.impl

import java.util.HashMap
import javax.jws.{WebService, WebMethod}
import org.aphreet.c3.platform.remote.api.access.{PlatformAccessAdapter}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.remote.api.ws.PlatformWSAccessEndpoint
import org.springframework.web.context.support.SpringBeanAutowiringSupport

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 1:32:15 PM
 * To change this template use File | Settings | File Templates.
 */


@Component
@WebService{val serviceName="AccessService", val targetNamespace="remote.c3.aphreet.org"}
class PlatformWSAccessEndpointImpl extends SpringBeanAutowiringSupport with PlatformWSAccessEndpoint{

  private var accessAdapter:PlatformAccessAdapter = null

  @Autowired
  private def setAccessAdapter(adapter:PlatformAccessAdapter) = {accessAdapter = adapter}

  @WebMethod
  override def getResourceAsString(ra:String):String = accessAdapter.getResourceAsString(ra)

  @WebMethod
  override def getMetadata(ra:String):HashMap[String, String] = accessAdapter.getMetadata(ra)

  @WebMethod{val exclude=true}
  override def $tag:Int = super.$tag

}