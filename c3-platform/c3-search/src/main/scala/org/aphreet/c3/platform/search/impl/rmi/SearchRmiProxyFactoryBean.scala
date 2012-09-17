
package org.aphreet.c3.platform.search.impl.rmi

import org.springframework.remoting.rmi.RmiProxyFactoryBean


class SearchRmiProxyFactoryBean extends RmiProxyFactoryBean{

  override def getBeanClassLoader:ClassLoader = getClass.getClassLoader
}