package org.aphreet.c3.platform.search

class SearchQueryException(message: String, cause: Throwable) extends RuntimeException(message, cause){

  def this(message: String) = this(message, null)

}
