package org.aphreet.c3.platform.search

import junit.framework.Assert._
import org.aphreet.c3.platform.search.api.SearchResultElement

class BasicContentSearchTestCase extends AbstractSearchTestCase{

  def searchQuery = "hello"

  def resources = List(
    resource("address0", "hello"),
    resource("address1", "Hello, name1"),
    resource("address3", "Hello, name2", Map(), "dsds")
  )

  override
  def fieldWeights = Map("content" -> 1)

  def verifyResults(found: List[SearchResultElement]) {
    assertEquals(List("address0", "address1"), found.map(e => e.address))
  }
}
