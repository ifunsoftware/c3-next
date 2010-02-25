package org.aphreet.c3.platform.client.common

import org.springframework.remoting.jaxws.JaxWsPortProxyFactoryBean
import java.net.URL

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 2:11:36 AM
 * To change this template use File | Settings | File Templates.
 */

trait SpringWsAccessor{

  def obtainWebService[T](url:String, service:String, namespace:String, port:String, clazz:Class[T]):T = {

    val factory:JaxWsPortProxyFactoryBean = new JaxWsPortProxyFactoryBean
    factory.setServiceInterface(clazz)
    factory.setWsdlDocumentUrl(new URL(url + "/" + service + "?WSDL"))
    factory.setNamespaceUri(namespace)
    factory.setServiceName(service)
    factory.setUsername("admin")
    factory.setPassword("password")
    factory.setPortName(port)
    factory.setMaintainSession(true)
    factory.afterPropertiesSet

    factory.getObject.asInstanceOf[T]
  }

}
