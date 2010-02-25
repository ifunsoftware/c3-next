package org.aphreet.c3.platform.remote.impl

import org.aphreet.c3.platform.remote.api.access.PlatformAccessAdapter
import java.util.HashMap
import scala.collection.jcl.{HashMap => JMap}
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.access.PlatformAccessEndpoint
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.remote.api.RemoteException

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 10:11:50 PM
 * To change this template use File | Settings | File Templates.
 */

@Component("platformAccessAdapter")
class PlatformAccessAdapterImpl extends PlatformAccessAdapter{

  private var accessEndpoint:PlatformAccessEndpoint = null

  @Autowired
  private def setAccessEndpoint(endpoint:PlatformAccessEndpoint) = {accessEndpoint= endpoint}

  def getResourceAsString(ra:String):String = {
    try{
      val resource = accessEndpoint get ra

      if(resource != null)
        resource.toString
      else null

    }catch{
      case e => throw new RemoteException(e)
    }
  }

  def getMetadata(ra:String):HashMap[String, String] = {

    try{

      val resource = accessEndpoint get ra

      if(resource != null){
        val map = new JMap[String, String]

        map ++ resource.metadata

        map.underlying
      }else{
        null
      }
    }catch{
      case e=> throw new RemoteException(e)
    }
  }

}