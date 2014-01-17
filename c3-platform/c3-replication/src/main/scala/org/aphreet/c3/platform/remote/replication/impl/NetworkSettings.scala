package org.aphreet.c3.platform.remote.replication.impl

import scala.collection.JavaConversions._
import java.net.{InetAddress, NetworkInterface}

/**
 * Author: Mikhail Malygin
 * Date:   1/17/14
 * Time:   4:19 PM
 */
object NetworkSettings {

  lazy val replicationBindAddress = resolveBindAddress()
  
  lazy val replicationBindPort = resolveBindPort()
  
  private def resolveBindPort(): String = {
    getSystemProperty("c3.replication.address") match {
      case Some(value) => value
      case None => "2552"
    }
  }
  
  private def resolveBindAddress(): String = {
    getConfiguredAddress match {
      case Some(value) => value
      case None => nonLoacalIpAddresses().headOption match {
        case Some(value) => value
        case None => "127.0.0.1"
      }
    }
  }

  private def getSystemProperty(name: String): Option[String] = {
    val value = System.getProperty(name)
    if(value != null){
      Some(value)
    }else{
      None
    }
  }
  
  private def getConfiguredAddress:Option[String] = {
    getSystemProperty("c3.replication.address")
  }

  private def nonLoacalIpAddresses(): List[String] = {

    val interfaceAddresses = NetworkInterface.getNetworkInterfaces
      .toList.sortBy(_.getName).map(iface =>
      iface.getInetAddresses.filter(!isLocalAddress(_)).toList
    )

    interfaceAddresses.flatten.map(_.getHostAddress)
  }

  private def isLocalAddress(address: InetAddress): Boolean = {
    address.isLinkLocalAddress || address.isLoopbackAddress
  }
}
