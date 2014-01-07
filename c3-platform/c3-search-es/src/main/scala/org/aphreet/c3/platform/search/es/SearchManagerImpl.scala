package org.aphreet.c3.platform.search.es

import org.springframework.stereotype.Component
import org.aphreet.c3.platform.search.api.{SearchResultFragment, SearchResultElement, SearchManager, SearchResult}
import org.aphreet.c3.platform.common.{Logger, WatchedActor}
import javax.annotation.{PostConstruct, PreDestroy}
import scala.util.control.Exception._
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.resource.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.access.ResourceUpdatedMsg
import org.aphreet.c3.platform.common.msg.RegisterNamedListenerMsg
import org.aphreet.c3.platform.access.ResourceDeletedMsg
import org.aphreet.c3.platform.access.ResourceAddedMsg
import scala.Some
import scala.collection.JavaConversions._
import org.elasticsearch.common.settings.{Settings, ImmutableSettings}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import java.util
import org.elasticsearch.common.Base64
import org.elasticsearch.action.admin.indices.exists.indices.{IndicesExistsResponse, IndicesExistsRequest}
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.{QueryStringQueryBuilder, QueryBuilders}
import org.elasticsearch.action.delete.DeleteResponse

/**
 * Need plugin
 * https://github.com/elasticsearch/elasticsearch-mapper-attachments
 */
@Component("searchManager")
class SearchManagerImpl extends SearchManager with WatchedActor {

  val log = Logger(getClass)

  var esClient: TransportClient = null

  val indexName: String = "resources-index"
  val docName: String = "resource"

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
    val settings: Settings = ImmutableSettings.settingsBuilder().put("cluster.name", "c3cluster").build()
    val transportClient: TransportClient = new TransportClient(settings)
    esClient = transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300))
    this.start()

    val resp: IndicesExistsResponse = esClient.admin().indices().exists(new IndicesExistsRequest(indexName)).actionGet()
     log info "mapping: \n: " + mapping()

    if (!resp.isExists) {
      esClient.admin().indices().prepareCreate(indexName).setSettings(
        ImmutableSettings.settingsBuilder()
          .put ("index.mapping.attachment.ignore_errors",false)
          .put("number_of_shards", 1)
          .put("index.numberOfReplicas", 0))
        .addMapping(docName, mapping())
        .execute().actionGet()

      log info "mapping changed"
    }
    accessMediator ! RegisterNamedListenerMsg(this, 'SearchManager)
  }

  def mapping(): String = {
    scala.io.Source.fromInputStream(getClass.getResourceAsStream("/config/index-mapping.json")).getLines().mkString
  }

  def search(domain: String, text: String): SearchResult = {
    log debug "Search called with query: " + text

    if (esClient == null) {
      log debug "ES client is null"
      SearchResult(text, new Array[SearchResultElement](0))
    } else {
      val queryBuilder:QueryStringQueryBuilder = QueryBuilders.queryString(text)

      val resp:SearchResponse = esClient.prepareSearch(indexName)
        .setQuery(queryBuilder )
        .addHighlightedField("document")
        .setHighlighterPreTags("<b>")
        .setHighlighterPostTags("</b>")
        .addHighlightedField("*")
        .addFields("*","address")
        .execute()
        .actionGet()

      val searchResults = resp.getHits.hits().map(hit => {
        println(hit)
        if (!hit.getFields.containsKey("address"))  {
           None
        } else {
           val score = hit.getScore
           val address = hit.getFields.get("address").values().get(0).asInstanceOf[String]
           val searchResultFragment = mapAsScalaMap(hit.getHighlightFields).map(e =>  SearchResultFragment(e._1, e._2.getFragments.map(_.string()))).toArray
           Some(SearchResultElement(address, null, score, searchResultFragment))
        }
      }).flatten.toList.toArray
      SearchResult(text, searchResults)
    }
  }

  def flushIndexes() {}

  def deleteIndexes() {}

  def dumpIndex(path: String) {}

  def act() {

    println("In the receive loop!")

    while (true) {
      receive {
        case ResourceAddedMsg(resource, source) =>
          println("Got resource added!")
          this ! IndexMsg(resource)

        case ResourceUpdatedMsg(resource, source) =>
          println("Got resource updated!")
          this ! DeleteFromIndexMsg(resource.address)
          this ! IndexMsg(resource)

        case ResourceDeletedMsg(address, source) =>
          println("Got resource deleted!")
          this ! DeleteFromIndexMsg(address)

        case DeleteFromIndexMsg(address) =>
          handling(classOf[Throwable]).by(e => {
            log.warn("Failed to index resource " + address, e)
            this ! ResourceDeleteFromIndexFailed(address)
          }).apply {
            log.trace("Got request to delete resource from index {}", address)
            this ! ResourceDeletedFromIndexMsg(address, removeResourceFromIndex(address))
          }

        case IndexMsg(resource) =>
          handling(classOf[Throwable]).by(e => {
            log.warn("Failed to index resource " + resource.address, e)
            this ! ResourceIndexingFailed(resource.address)
          }).apply {
            log.trace("Got request to index {}", resource.address)

            if (shouldIndexResource(resource)) {
              log.debug("Indexing resource {}", resource.address)
              this ! ResourceIndexedMsg(resource.address, indexResource(resource))
            } else {
              log.debug("No need to index resource {}", resource.address)
            }
          }
        }
    }
  }

  def removeResourceFromIndex(address: String): Map[String, String] = {
    log.debug("{}: Deleting resource {}", address)
    var response: DeleteResponse = null
    try {
      response = esClient.prepareDelete(indexName, docName, address).execute().actionGet()
    } catch {
      case e: Exception => log error "exception caught: " + e.printStackTrace()
    }

    log.debug("Resource deleted from index ({}) : {}", address, response)
    val m: Map[String, String] = collection.immutable.HashMap("address" -> address, "2" -> "3")
    m
  }

  def indexResource(resource: Resource): Map[String, String] = {
    log.debug("{}: Indexing resource {}", resource.address)
    var response: IndexResponse = null
    try {
      // val language = getLanguage(resource.metadata, extractedDocument)
      val metadataMap = new util.HashMap[String, String]()
      resource.metadata.asMap.foreach {
        keyVal => {
          println(keyVal._1 + "=" + keyVal._2)
          metadataMap.put(keyVal._1, keyVal._2)
        }
      }


      val doc: XContentBuilder = XContentFactory.jsonBuilder()
        .startObject()
        .field("address", resource.address)
        .field("metadata", metadataMap)
        .field("document", Base64.encodeBytes(resource.versions.head.data.getBytes))
        .endObject()

      response = esClient.prepareIndex(indexName, docName, resource.address)
        .setSource(doc)
        .execute()
        .actionGet()

      log.debug("ES document: {}", doc)
    } catch {
      case e: Exception => log error "exception caught: " + e.printStackTrace()
    }

    log.debug("Resource writen to index ({}) : {}", resource.address, response.toString)
    val m: Map[String, String] = collection.immutable.HashMap("address" -> resource.address, "2" -> "3")
    m
  }

  def shouldIndexResource(resource: Resource): Boolean = {
    resource.systemMetadata("c3.skip.index") match {
      case Some(x) => false
      case None => true
    }
  }

  case class ResourceDeleteFromIndexFailed(address: String)

  case class ResourceIndexingFailed(address: String)

  case class ResourceIndexedMsg(address: String, extractedMetadata: Map[String, String])

  case class IndexMsg(resource: Resource)

  case class ResourceDeletedFromIndexMsg(address: String, extractedMetadata: Map[String, String])

  case class DeleteFromIndexMsg(address: String)

}
