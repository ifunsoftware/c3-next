package org.aphreet.c3.platform.search.es

import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.node.{Node, NodeBuilder}
import scala.Option
import scala.Predef._
import scala.Some
import scala.collection.mutable


trait ESClientFactory {

  def createClient(settings: Settings, esHost: Option[String]): Client

  def destroyClient(client: Client)

}

object ESClientFactoryImpl extends ESClientFactory {

  def createClient(settings: Settings, esHost: Option[String]): Client = {
    val transportClient = new TransportClient(settings)

    val host = esHost match {
      case Some(value) => value
      case None => "localhost"
    }

    transportClient.addTransportAddress(new InetSocketTransportAddress(host, 9300))

    transportClient
  }

  override def destroyClient(client: Client) {
    client.asInstanceOf[TransportClient].threadPool().shutdown()
    client.close()
  }
}

object ESEmbeddedClientFactoryImpl extends ESClientFactory {

  private val clients = new mutable.HashMap[Client, Node] with
    mutable.SynchronizedMap[Client, Node]

  def createClient(settings: Settings, esHost: Option[String]): Client = {
    val node = NodeBuilder.nodeBuilder().settings(settings).data(true).local(true).build()
    val client = node.client()

    clients.put(client, node)

    client
  }

  def destroyClient(client: Client) {
    clients.get(client).map(_.close())
    clients.remove(client)
    client.close()
  }
}
