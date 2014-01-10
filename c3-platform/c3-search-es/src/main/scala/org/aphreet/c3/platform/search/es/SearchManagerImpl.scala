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
import scala.{Array, None, Some}
import scala.collection.JavaConversions._
import org.elasticsearch.common.settings.{Settings, ImmutableSettings}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import java.util
import org.elasticsearch.common.Base64
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.index.query.{QueryStringQueryBuilder, QueryBuilders}
import org.elasticsearch.action.delete.DeleteResponse
import scala.io.Source
import org.aphreet.c3.platform.config.{PropertyChangeEvent, SPlatformPropertyListener}
import java.io.File

/**
 * Need plugin
 * https://github.com/elasticsearch/elasticsearch-mapper-attachments
 */
@Component("searchManager")
class SearchManagerImpl extends SearchManager with WatchedActor  with SPlatformPropertyListener {

  val log = Logger(getClass)

  val ES_HOST = "ES_HOST"

  val ES_CLUSTER_NAME = "ES_CLUSTER_NAME"

  var esHost:String = "localhost"

  var esClusterName:String = "c3cluster"

  var esClient: Option[TransportClient] = None

  val indexName: String = "resources-index"
  val docName: String = "resource"

  @Autowired
  var accessMediator: AccessMediator = _

  @PreDestroy
  def destroy() {
    log info "Destroying SearchManager"
    esClient.map(_.close())
  }

  @PostConstruct
  def init() {
    log info "init SearchManagerImpl es"
    val settings = ImmutableSettings.settingsBuilder().put("cluster.name", esClusterName).build()
    val transportClient = new TransportClient(settings)
    esClient = Some(transportClient.addTransportAddress(new InetSocketTransportAddress(esHost, 9300)))
    this.start()

    val exists = esClient.flatMap(client =>
      Some(client.admin().indices().exists(new IndicesExistsRequest(indexName)).actionGet()))
    .flatMap(response => Some(response.isExists))

    exists match {
      case None => esClient.flatMap(client => Some(client.admin().indices().prepareCreate(indexName).setSettings(
        ImmutableSettings.settingsBuilder()
          .put ("index.mapping.attachment.ignore_errors",false)
          .put("number_of_shards", 1)
          .put("index.numberOfReplicas", 0))
        .addMapping(docName, mapping)
        .execute().actionGet()))
        log info "mapping changed"

      case Some(exists) => ;
    }

    accessMediator ! RegisterNamedListenerMsg(this, 'SearchManager)
  }

  lazy val mapping = Source.fromInputStream(getClass.getResourceAsStream("/config/index-mapping.json")).getLines().mkString

  def search(domain: String, text: String): SearchResult = {
    log debug "Search called with query: " + text

    val queryBuilder:QueryStringQueryBuilder = QueryBuilders.queryString(text)

    val resp = esClient.flatMap(client => Some(client.prepareSearch(indexName)
      .setQuery(queryBuilder )
      .addHighlightedField("document")
      .setHighlighterPreTags("<b>")
      .setHighlighterPostTags("</b>")
      .addHighlightedField("*")
      .addFields("*","address")
      .execute()
      .actionGet()))

    val searchResults = resp.flatMap(resp => Some(resp.getHits.hits().map(hit => {
      log debug "hit " + hit
      if (!hit.getFields.containsKey("address"))  {
         None
      } else {
         val score = hit.getScore
         val address = hit.getFields.get("address").values().get(0).asInstanceOf[String]
         val searchResultFragment = mapAsScalaMap(hit.getHighlightFields).map(e =>  SearchResultFragment(e._1, e._2.getFragments.map(_.string()))).toArray
         Some(SearchResultElement(address, null, score, searchResultFragment))
      }
    }).flatten.toList.toArray))

    searchResults match {
      case Some(results) =>  SearchResult(text, results)
      case None => SearchResult(text, new Array[SearchResultElement](0))
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
    var response: Option[DeleteResponse] = None
    try {
      response = esClient.flatMap(client => Some(client.prepareDelete(indexName, docName, address).execute().actionGet() ))
    } catch {
      case e: Exception => log error "exception caught: " + e.printStackTrace()
    }

    log.debug("Resource deleted from index ({}) : {}", address, response)
    Map("address" -> address, "2" -> "3")
  }

  def indexResource(resource: Resource): Map[String, String] = {
    log.debug("{}: Indexing resource {}", resource.address)
    var response: Option[IndexResponse] = None
    try {
      // val language = getLanguage(resource.metadata, extractedDocument)
      val metadataMap = new util.HashMap[String, String]()
      resource.metadata.asMap.foreach {
        keyVal => {
          log debug keyVal._1 + "=" + keyVal._2
          metadataMap.put(keyVal._1, keyVal._2)
        }
      }

      val doc: XContentBuilder = XContentFactory.jsonBuilder()
        .startObject()
        .field("address", resource.address)
        .field("metadata", metadataMap)
        .field("document", Base64.encodeBytes(resource.versions.head.data.getBytes))
        .endObject()

      response = esClient.flatMap(client => Some (client.prepareIndex(indexName, docName, resource.address)
        .setSource(doc)
        .execute()
        .actionGet()))

      log.debug("ES document: {}", doc)
    } catch {
      case e: Exception => log error "exception caught: " + e.printStackTrace()
    }

    log.debug("Resource writen to index ({}) : {}", resource.address, response.toString)
    Map("address" -> resource.address,
      "es_metadata" -> "unknown") //TODO get indexed doc metadata (author, title, ...)

  }

  def shouldIndexResource(resource: Resource): Boolean = {
    resource.systemMetadata("c3.skip.index").isEmpty
  }

  def propertyChanged(event: PropertyChangeEvent): Unit = {
    event.name match {
      case ES_HOST =>
        val newHost = event.newValue
        if (esHost != newHost) {
          log info "New esHost address : " + newHost
          esHost = newHost
          init()
        }
      case ES_CLUSTER_NAME =>
        val newClusterName = event.newValue
        if (esClusterName != newClusterName) {
           log info "New esClusterName : " + newClusterName
           esClusterName = newClusterName
           init()
        }
    }
  }

  override def listeningForProperties: Array[String] = Array(
    ES_HOST, ES_CLUSTER_NAME
  )

  def defaultValues: Map[String, String] = Map(
    ES_HOST -> ES_HOST,
    ES_CLUSTER_NAME -> ES_CLUSTER_NAME
  )

  case class ResourceDeleteFromIndexFailed(address: String)

  case class ResourceIndexingFailed(address: String)

  case class ResourceIndexedMsg(address: String, extractedMetadata: Map[String, String])

  case class IndexMsg(resource: Resource)

  case class ResourceDeletedFromIndexMsg(address: String, extractedMetadata: Map[String, String])

  case class DeleteFromIndexMsg(address: String)
}
