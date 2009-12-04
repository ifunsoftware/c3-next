package org.aphreet.c3.platform.search.impl

import org.aphreet.c3.platform.search.Searcher
import org.aphreet.c3.platform.resource.Resource

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import java.util.{List, ArrayList}

import javax.annotation.{PreDestroy, PostConstruct}

@Component
class SearcherImpl extends Searcher{

  val log = org.apache.commons.logging.LogFactory getLog getClass
  
  var searchManager:SearchManager = null
  
  @Autowired
  def setSearchManager(manager:SearchManager) = {
    searchManager = manager
  }
  
  
  @PostConstruct
  def init{
    searchManager.registerSearcher(this)
  }
  
  def search(query:String):List[String] = {
    log info "Search stub"
    new ArrayList
  }
  
  def index(resource:Resource) = {
    log info "Index stub"
  }
  
  @PreDestroy
  def destroy{
	searchManager.unregisterSearcher(this)
  }
  
}
