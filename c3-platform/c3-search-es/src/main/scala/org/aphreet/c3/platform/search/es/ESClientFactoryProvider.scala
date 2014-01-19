package org.aphreet.c3.platform.search.es

trait ESClientFactoryProvider {

  def clientFactory: ESClientFactory

}

trait ESTransportClientFactoryProvider extends ESClientFactoryProvider{
  def clientFactory = ESClientFactoryImpl
}

trait ESEmbededClientFactoryProvider extends ESClientFactoryProvider{
  def clientFactory = ESEmbeddedClientFactoryImpl
}
