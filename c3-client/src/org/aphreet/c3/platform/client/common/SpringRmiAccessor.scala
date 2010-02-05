package org.aphreet.c3.platform.client.common

import org.springframework.remoting.rmi.RmiProxyFactoryBean

trait SpringRmiAccessor {

  def obtainRmiService[T](url:String, clazz:Class[T]):T = {
    val rmiBean = new RmiProxyFactoryBean
    rmiBean.setServiceUrl(url)
    rmiBean.setServiceInterface(clazz)
    rmiBean.afterPropertiesSet
    rmiBean.getObject.asInstanceOf[T]
  }
}
