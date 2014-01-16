package org.aphreet.c3.platform.search.es

import junit.framework.Assert._
import junit.framework.TestCase
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.common.{DefaultComponentLifecycle, Logger}
import org.aphreet.c3.platform.config._
import org.aphreet.c3.platform.resource._
import org.aphreet.c3.platform.search.api.SearchResultElement
import org.easymock.EasyMock._
import scala.collection.Map
import scala.collection.mutable

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

  trait DependencyProvider extends AccessComponent
  with PlatformConfigComponent
  with DefaultComponentLifecycle{
    val accessManager = createMock(classOf[AccessManager])

    val accessMediator = createMock(classOf[AccessMediator])

    val platformConfigManager = createMock(classOf[PlatformConfigManager])

    val configPersister = createMock(classOf[ConfigPersister])
  }

  val searchComponent = new Object with DependencyProvider
    with ESTransportClientFactoryProvider
    with SearchComponentImpl

  val searchManagerImpl = searchComponent.searchManager

  val resource:Resource = new Resource()
  val parentPath:String = "parentPath"
  val log = Logger (getClass)

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
