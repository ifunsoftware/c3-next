package org.aphreet.c3.platform.search

import junit.framework.Assert._
import org.aphreet.c3.platform.search.api.SearchResultElement

class ListMetadataRangesSearchTestCase extends AbstractSearchTestCase{

  def searchQuery = "tags:tag2 tags:tag1"

  def resources = List(
    resource("address0", "hello", Map("tags" -> "[tag1,tag2,tag3]")),
    resource("address1", "Hello, name1", Map("tags" -> "[tag1,tag2]")),
    resource("address2", "Hi, man!", Map("tags" -> "[tag2,tag3]")),
    resource("address3", "Hello, name2", Map("tags" -> "[tag4,tag3]"))
  )

  def verifyResults(found: List[SearchResultElement]) {
    assertEquals(List("address1", "address0", "address2"), found.map(e => e.address))
  }
}