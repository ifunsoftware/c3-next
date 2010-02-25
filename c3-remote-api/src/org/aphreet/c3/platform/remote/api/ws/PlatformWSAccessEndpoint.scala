package org.aphreet.c3.platform.remote.api.ws

import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import java.util.HashMap
import javax.jws.{WebService, WebMethod}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 24, 2010
 * Time: 12:19:31 AM
 * To change this template use File | Settings | File Templates.
 */

@WebService{val serviceName="AccessService", val targetNamespace="remote.c3.aphreet.org"}
trait PlatformWSAccessEndpoint extends PlatformAccessService {

  @WebMethod
  def getMetadata(ra:String):HashMap[String, String]

  @WebMethod
  def getResourceAsString(ra:String):String

}