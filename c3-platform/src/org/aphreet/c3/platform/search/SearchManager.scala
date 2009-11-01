package org.aphreet.c3.platform.search

import org.aphreet.c3.platform.resource.Resource

trait SearchManager {

  def search(query:String):List[Resource]
  
  def index(resource:Resource)
  
  def registerSearcher(searcher:Searcher)
  
  def unregisterSearcher(searcher:Searcher)
 
  def isSearchAvaliable:Boolean
}
