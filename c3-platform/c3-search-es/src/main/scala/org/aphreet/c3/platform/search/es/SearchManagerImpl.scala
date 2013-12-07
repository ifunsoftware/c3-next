package org.aphreet.c3.platform.search.es

import org.springframework.stereotype.Component
import org.aphreet.c3.platform.search.api.{SearchManager}
import org.aphreet.c3.platform.common.{Logger, WatchedActor}
import javax.annotation.{PostConstruct, PreDestroy}
import scala.util.control.Exception._
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.resource.{MetadataHelper, Resource}
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.access.ResourceUpdatedMsg
import org.aphreet.c3.platform.common.msg.RegisterNamedListenerMsg
import org.aphreet.c3.platform.search.api.SearchResult
import org.aphreet.c3.platform.access.ResourceDeletedMsg
import org.aphreet.c3.platform.access.ResourceAddedMsg
import scala.Some
import org.elasticsearch.common.settings.{Settings, ImmutableSettings}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import scala.collection.mutable
import java.util
import scala.collection.mutable.ArrayBuffer


@Component("searchManager")
class SearchManagerImpl extends SearchManager with WatchedActor {

  val log = Logger(getClass)

  var esClient:TransportClient = null

  @Autowired
  var accessMediator: AccessMediator = _

  @PreDestroy
  def destroy() {
    log info "Destroying SearchManager"
    if (esClient != null)
      esClient.close()
  }

  @PostConstruct
  def init() {
    log info "init SearchManagerImpl es"
    val settings:Settings = ImmutableSettings.settingsBuilder().put("cluster.name", "c3cluster").build()
    val transportClient:TransportClient = new TransportClient(settings)
    esClient = transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300))

    val map:XContentBuilder = XContentFactory.jsonBuilder().startObject()
      .startObject("attachment")
      .startObject("properties")
      .startObject("file")
      .field("type", "attachment")
      .startObject("fields")
      .startObject("title")
      .field("store", "yes")
      .endObject()
      .startObject("file")
      .field("term_vector","with_positions_offsets")
      .field("store","yes")
      .endObject()
      .endObject()
      .endObject()
      .endObject()
      .endObject()
    this.start()
    esClient.admin().indices().prepareCreate("resources").setSettings(
      ImmutableSettings.settingsBuilder()
        .put("number_of_shards", 1)
        .put("index.numberOfReplicas", 1))
      .addMapping("attachment", map).execute().actionGet()
    accessMediator ! RegisterNamedListenerMsg(this, 'SearchManager)
  }

  def search(domain: String, query: String): SearchResult = {

    log debug "Search called with query: " + query

    if(esClient == null){
      log debug "Solr server is null"
      SearchResult(query, null)
    }
    else  SearchResult(query, null)//solr.search(domain, query)
  }

  def flushIndexes() {}

  def deleteIndexes() {}

  def dumpIndex(path: String) {}

  def act() {

    println("In the receive loop!")

    while (true) {
      receive {
        case ResourceAddedMsg(resource, source) => {
          println("Got resource added!")
          this ! IndexMsg(resource)
        }

        case ResourceUpdatedMsg(resource, source) => ;

        case ResourceDeletedMsg(address, source) => ;

        case IndexMsg(resource) => {
          handling(classOf[Throwable]).by(e => {
            log.warn("Failed to index resource " + resource.address, e)
            this ! ResourceIndexingFailed(resource.address)
          }).apply{
            log.trace("Got request to index {}", resource.address)

            if(shouldIndexResource(resource)){

              log.debug("Indexing resource {}", resource.address)

              this ! ResourceIndexedMsg(resource.address, indexResource(resource))

            }else{
              log.debug("No need to index resource {}", resource.address)
            }
          }
        }


      }
    }
  }

  def indexResource(resource: Resource): Map[String, String] = {
    log.debug("{}: Indexing resource {}", resource.address)
    var response:IndexResponse = null
    try{
      // val language = getLanguage(resource.metadata, extractedDocument)
      val metadataMap = new util.HashMap[String,String]()
      resource.metadata.asMap.foreach{keyVal => {
        println(keyVal._1 + "=" + keyVal._2)
        metadataMap.put(keyVal._1, keyVal._2)
      }}

      val doc:XContentBuilder  = XContentFactory.jsonBuilder()
        .startObject()
        .field("address", resource.address)
        .field("metadata", metadataMap)
        .field("attachment", org.elasticsearch.common.Base64.encodeBytes(resource.toByteArray))
        .endObject()

      esClient.prepareIndex("twitter", "tweet", "1")
        .setSource(doc)
        .execute()
        .actionGet()

      log.debug("ES document: {}", doc)
    }

    log.debug("Resource writen to index ({}) : {}", resource.address, response.toString)
    val m: Map[String, String] = collection.immutable.HashMap("address" -> resource.address, "2" -> "3")
    m
  }

  def shouldIndexResource(resource:Resource):Boolean = {
    resource.systemMetadata("c3.skip.index") match{
      case Some(x) => false
      case None => true
    }
  }

  case class ResourceIndexingFailed(address: String)
  case class ResourceIndexedMsg(address: String, extractedMetadata: Map[String, String])
  case class IndexMsg(resource: Resource)
}
