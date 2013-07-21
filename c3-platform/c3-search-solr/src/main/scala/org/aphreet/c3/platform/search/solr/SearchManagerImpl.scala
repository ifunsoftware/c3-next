package org.aphreet.c3.platform.search.solr

import org.springframework.stereotype.Component
import org.aphreet.c3.platform.search.api.{SearchManager}
import org.aphreet.c3.platform.common.{Logger, WatchedActor}
import javax.annotation.{PostConstruct, PreDestroy}
import org.apache.solr.client.solrj.SolrServer
import org.apache.solr.client.solrj.impl.HttpSolrServer
import scala.util.control.Exception._
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.resource.Resource
import org.apache.solr.common.SolrInputDocument
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.access.ResourceUpdatedMsg
import org.aphreet.c3.platform.common.msg.RegisterNamedListenerMsg
import org.aphreet.c3.platform.search.api.SearchResult
import org.aphreet.c3.platform.access.ResourceDeletedMsg
import org.aphreet.c3.platform.access.ResourceAddedMsg
import scala.Some


@Component("searchManager")
class SearchManagerImpl extends SearchManager with WatchedActor {

  val log = Logger(getClass)

  val URL = "http://localhost:8989/solr"

  var solr:SolrServer = null

  @Autowired
  var accessMediator: AccessMediator = _

  @PreDestroy
  def destroy() {
    log info "Destroying SearchManager"
    if (solr != null)
      solr.shutdown()
  }

  @PostConstruct
  def initialize(numberOfIndexers:Int) {
     log info "init SearchManagerImpl solr"
     solr = new HttpSolrServer(URL)

     this.start()
     accessMediator ! RegisterNamedListenerMsg(this, 'SearchManager)
  }



  def search(domain: String, query: String): SearchResult = {

    log debug "Search called with query: " + query

    if(solr == null){
      log debug "Solr server is null"
      SearchResult(query, null)
    }
    else  SearchResult(query, null)//solr.search(domain, query)
  }

  def flushIndexes() {}

  def deleteIndexes() {}

  def dumpIndex(path: String) {}

  def act() {
    while (true) {
      receive {
        case ResourceAddedMsg(resource, source) => this ! IndexMsg(resource)

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
    val document:SolrInputDocument = new SolrInputDocument()

    try{
      // val language = getLanguage(resource.metadata, extractedDocument)
      document.addField("address", resource.address)
      document.addField("metadata", resource.metadata)
      log.debug("Solr document: {}", document.toString)
      solr.add(document)
    }

    log.debug("Resource writen to index ({})", resource.address)
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
