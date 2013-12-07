package org.aphreet.c3.platform.search.es

import junit.framework.TestCase
import org.aphreet.c3.platform.resource._
import junit.framework.Assert._
import scala.collection.mutable
import scala.collection.Map
import org.aphreet.c3.platform.access.{AccessMediator, ResourceAddedMsg}
import org.easymock.EasyMock._


class SearchManagerImplTest extends TestCase {

  var searchManagerImpl:SearchManagerImpl = new SearchManagerImpl
  val resource:Resource = new Resource()
  val parentPath:String = "parentPath"

  override def setUp() {
    val resourceMetadata:Map[String, String] = Map(("tags", "[cats,scala,cycling]"))
    resource.metadata = new Metadata(new mutable.HashMap() ++= resourceMetadata)
    resource.address = "someAddress"
    resource.systemMetadata = new Metadata(new mutable.HashMap() ++=  Map(("c3.fs.parent", parentPath),("c3.fs.nodetype", "file")))

    resource.addVersion(ResourceVersion(DataStream.create("Try out elasticsearch")))

    searchManagerImpl.accessMediator = createMock(classOf[AccessMediator])
    searchManagerImpl.init()
  }

  def testAddResource() {
    val initialMap = Map(("cats",2), ("scala",2), ("cycling", 2))
    val mapString = MetadataHelper.writeTagMap(initialMap, (key: String, value: Int) => {key + ":" + value})

    val map =  MetadataHelper.parseTagMap(mapString, (tagInfo: String) => {
      (tagInfo.split(":")(0), tagInfo.split(":")(1).toInt)
    }).toMap[String, Int]
    assertEquals(initialMap, map)
    searchManagerImpl ! ResourceAddedMsg(resource, Symbol("source"))

    Thread.sleep(1000000)
    //assertEquals(Some(MetadataHelper.writeTagMap(Map(("scala",1),("cycling", 1),("cats",1)).toMap[String, Int], (key: String, value: Int) => {key + ":" + value})), parent.metadata(TagManager.TAGS_FIELD))
  }
}
