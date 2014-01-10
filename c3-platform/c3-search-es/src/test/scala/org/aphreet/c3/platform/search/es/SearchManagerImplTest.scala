package org.aphreet.c3.platform.search.es

import junit.framework.TestCase
import org.aphreet.c3.platform.resource._
import junit.framework.Assert._
import scala.collection.mutable
import scala.collection.Map
import org.aphreet.c3.platform.access.{ResourceDeletedMsg, AccessMediator, ResourceAddedMsg}
import org.easymock.EasyMock._
import java.io.File
import scala.io.Source
import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.search.api.SearchResultElement
import org.aphreet.c3.platform.config.{PropertyChangeEvent, SetPropertyMsg}

/**
//hightlight
{
  "fields": [
    "*",
    "address"
  ],
  "query": {
    "query_string": {
      "query": "ability"
    }
  },
  "highlight": {
    "pre_tags": [
      "<b>"
    ],
    "post_tags": [
      "</b>"
    ],
    "fields": {
      "document": {
        "number_of_fragments": 3
      }
    }
  }
}
 */
 class SearchManagerImplTest extends TestCase {

  var searchManagerImpl:SearchManagerImpl = new SearchManagerImpl
  val resource:Resource = new Resource()
  val parentPath:String = "parentPath"
  val log = Logger (getClass)

  override def setUp() {
    searchManagerImpl.accessMediator = createMock(classOf[AccessMediator])
    searchManagerImpl.init()
  }

  def resource(address:String, data:String, metadata:Map[String, String] = Map(), domain:String ):Resource = {
    val resource = new Resource
    resource.address = address
    resource.systemMetadata("c3.domain.id") = domain
    resource.addVersion(ResourceVersion(DataStream.create(data)))
    resource.metadata ++= metadata

    resource
  }

  def indexResource(resource:Resource) {
    searchManagerImpl ! ResourceAddedMsg(resource, Symbol("source"))
  }

  def removeResourceFromIndex(resource:Resource) {
    searchManagerImpl ! ResourceDeletedMsg("address1", Symbol("source"))
  }

  def testSearch(){
    searchManagerImpl.propertyChanged(new PropertyChangeEvent("ES_HOST", null, "localhost", this))
    searchManagerImpl.propertyChanged(new PropertyChangeEvent("ES_CLUSTER_NAME", null, "c3cluster", this))
    resources.foreach(indexResource)
    Thread.sleep(1000)
    verifyResults(searchManagerImpl.search("domain", searchQuery).elements.toList)
    resources.foreach(removeResourceFromIndex)
  }

  def searchQuery:String = "Tika"

  var resources:List[Resource] = List(
    resource("address1",
      scala.io.Source.fromInputStream(getClass.getResourceAsStream("/testXHTML.html")).getLines().mkString,
      new mutable.HashMap() ++= Map(("tags", "[cats,scala,cycling]")),
      "domain")
  )

  def verifyResults(found:List[SearchResultElement]) {
    println (found)
    assertEquals(List("address1"), found.map(e => e.address))
  }
}
