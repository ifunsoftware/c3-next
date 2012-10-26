package org.aphreet.c3.platform.search

import junit.framework.Assert._

class MetadataOnlySearchTestCase extends AbstractSearchTestCase{

  def searchQuery = "+content-type:message"

  def resources = List(
    resource("address0", "hello", Map("content-type" -> "message")),
    resource("address1", "Hello, name1", Map("content-type" -> "message")),
    resource("address2", "Hi, man!", Map("content-type" -> "message")),
    resource("address3", "Hello, name2", Map("content-type" -> "wiki"))
  )

  def verifyResults(found: List[SearchResultElement]) {
    assertEquals(List("address0", "address1", "address2"), found.map(e => e.address))
  }
}
