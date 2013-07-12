package org.aphreet.c3.platform.search

import junit.framework.Assert._
import org.aphreet.c3.platform.search.api.SearchResultElement

class MultipleMetadataSearchTestCase extends AbstractSearchTestCase{

  def searchQuery = "+content-type:message +author:user0"

  def resources = List(
    resource("address0", "hello", Map("content-type" -> "message", "author" -> "user0")),
    resource("address1", "Hello, name1", Map("content-type" -> "message", "author" -> "user1")),
    resource("address2", "Hi, man!", Map("content-type" -> "message", "author" -> "user0")),
    resource("address3", "Hello, name2", Map("content-type" -> "wiki", "author" -> "user1"))
  )

  def verifyResults(found: List[SearchResultElement]) {
    assertEquals(List("address0", "address2"), found.map(e => e.address))
  }
}
