package org.aphreet.c3.platform.search

import org.aphreet.c3.platform.resource.Resource

import java.util.List

trait Searcher {

  def search(query:String):List[String]
  
  def index(resource:Resource)
  
}
