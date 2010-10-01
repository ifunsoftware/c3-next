package org.aphreet.c3.platform.client.common

import org.springframework.remoting.jaxws.JaxWsPortProxyFactoryBean
import java.net.URL
import org.aphreet.c3.platform.client.management.connection.ConnectionException

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 2:11:36 AM
 * To change this template use File | Settings | File Templates.
 */

trait SpringWsAccessor {
  def obtainWebService[T](url: String, user: String, password: String, service: String, namespace: String, port: String, clazz: Class[T], wsdlPath:String): T = {
    try {
      val factory: JaxWsPortProxyFactoryBean = new JaxWsPortProxyFactoryBean
      factory.setServiceInterface(clazz)
      factory.setWsdlDocumentUrl(new URL(url + "/" + wsdlPath + "?WSDL"))
      factory.setNamespaceUri(namespace)
      factory.setServiceName(service)
      factory.setUsername(user)
      factory.setPassword(password)
      factory.setPortName(port)
      factory.setMaintainSession(true)
      factory.afterPropertiesSet

      factory.getObject.asInstanceOf[T]
    } catch {
      case e: javax.xml.ws.WebServiceException => {
        throw new ConnectionException(e.getMessage)
      }
    }
  }

}
