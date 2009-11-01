package org.aphreet.c3.platform.search

import org.aphreet.c3.platform.resource.Resource

trait Searcher {

  def search(query:String):List[Resource]
  
  def index(resource:Resource)
  
}
