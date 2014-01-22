package org.aphreet.c3.platform.search

import junit.framework.Assert._
import org.aphreet.c3.platform.search.api.SearchResultElement

class MetadataSearchTestCase extends AbstractSearchTestCase{

  def searchQuery = "+hello +content-type:message"

  def resources = List(
    resource("address0", "hello", Map("content-type" -> "message")),
    resource("address1", "Hello, name1", Map("content-type" -> "message")),
    resource("address2", "Hi, man!", Map("content-type" -> "message")),
    resource("address3", "Hello, name2", Map("content-type" -> "wiki"))
  )

  override
  def fieldWeights = Map("content" -> 1)

  def verifyResults(found: List[SearchResultElement]) {
    assertEquals(List("address0", "address1"), found.map(e => e.address))
  }
}
