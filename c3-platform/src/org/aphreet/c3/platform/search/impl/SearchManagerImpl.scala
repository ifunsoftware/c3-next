package org.aphreet.c3.platform.search.impl

import java.util.List
import org.aphreet.c3.platform.resource.Resource

import org.springframework.stereotype.Component

@Component("searchManager")
class SearchManagerImpl extends SearchManager{

  var searcher:Searcher = null
  
  val log = org.apache.commons.logging.LogFactory.getLog(getClass)
  
  def search(query:String):List[String] = {
    searcher.search(query)
  }
  
  def index(resource:Resource) = searcher.index(resource)
  
  def registerSearcher(_searcher:Searcher) = {
    log info "registering searcher: " + _searcher.getClass.getSimpleName
    searcher = _searcher
  }
  
  def unregisterSearcher(_searcher:Searcher) = {
    log info "Unregistering searcher: " + searcher.getClass.getSimpleName
    searcher = null
  }
  
  def isSearchAvaliable:Boolean = (searcher != null)
}
