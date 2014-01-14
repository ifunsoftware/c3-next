package org.aphreet.c3.platform.tags.impl

import akka.actor.ActorSystem
import junit.framework.Assert._
import junit.framework.TestCase
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.actor.ActorComponent
import org.aphreet.c3.platform.common.msg.{UnregisterNamedListenerMsg, RegisterNamedListenerMsg}
import org.aphreet.c3.platform.common.{ThreadWatcher, DefaultComponentLifecycle}
import org.aphreet.c3.platform.filesystem.{Directory, Node}
import org.aphreet.c3.platform.resource.{MetadataHelper, Metadata, Resource}
import org.aphreet.c3.platform.tags.TagManager
import org.easymock.EasyMock._
import scala.Some
import scala.collection.Map
import scala.collection.mutable

class TagManagerImplTest extends TestCase {

    var tagManager: TagManager with ResourceOwner = null
    val resource:Resource = new Resource()
    val parent:Resource = new Resource()
    val parent2:Resource = new Resource()
    val parentPath:String = "parentPath"
    val parent2Path:String = "parent2Path"

    var app: AccessComponent with DefaultComponentLifecycle  = null

    override def setUp() {
      val resourceMetadata:Map[String, String] = Map((TagManager.TAGS_FIELD, "[cats,scala,cycling]"))

      val tagsInResource: Map[String, Int] = Map("cats" -> 1, "scala" -> 1, "cycling" -> 1)
      val deletedTags: String = MetadataHelper.writeTagMap(tagsInResource.toMap[String, Int], (key: String, value: Int) => {key + ":" + value})

      val dirMetadata:Map[String, String] = Map((TagManager.TAGS_FIELD, deletedTags))

      resource.metadata = new Metadata(new mutable.HashMap() ++= resourceMetadata)
      resource.address = "someAddress"
      resource.systemMetadata = new Metadata(new mutable.HashMap() ++=  Map((Node.NODE_FIELD_PARENT, parentPath),(Node.NODE_FIELD_TYPE, "file")))

      parent.metadata = new Metadata(new mutable.HashMap())
      parent.systemMetadata = new Metadata(new mutable.HashMap() ++=  Map((Node.NODE_FIELD_PARENT, parent2Path),(Node.NODE_FIELD_TYPE, "directory")))
      parent.address = "parent1Address"

      parent2.metadata = new Metadata(new mutable.HashMap() ++= dirMetadata)
      parent2.systemMetadata = new Metadata(new mutable.HashMap() ++=  Map((Node.NODE_FIELD_TYPE, "directory")))
      parent2.address = "parent2Address"

      val dir2 = Directory.emptyDirectory("someDir2", "")
      dir2.addChild("dir2", parent2.address, leaf = true)

      val parent2Node = Node.fromResource(parent2)
      val parent2Dir = parent2Node.asInstanceOf[Directory]
      parent2Dir.addChild("dir1", parent.address, leaf = true)

      val parentNode = Node.fromResource(parent)
      val parentDir = parentNode.asInstanceOf[Directory]
      parentDir.addChild("file", resource.address, leaf = true)

      val accessManagerMock = createMock(classOf[AccessManager])
      val accessMediatorMock = createMock(classOf[AccessMediator])

      expect(accessManagerMock.get(resource.address)).andReturn(resource).anyTimes
      expect(accessManagerMock.update(parent)).andReturn("").anyTimes
      expect(accessManagerMock.update(parent2)).andReturn("").anyTimes
      expect(accessManagerMock.get(parentPath)).andReturn(parent).anyTimes()
      expect(accessManagerMock.get(parent2Path)).andReturn(parent2).anyTimes()

      expect(accessMediatorMock.!(RegisterNamedListenerMsg(anyObject(), 'tagManager)))
      expect(accessMediatorMock.!(UnregisterNamedListenerMsg(anyObject(), 'tagManager)))

      replay(accessMediatorMock)
      replay(accessManagerMock)

      val actorRefFactory = ActorSystem()

      val module = new Object with DefaultComponentLifecycle
        with AccessComponent
        with ActorComponent
        with TagComponentImpl {

        def actorSystem = actorRefFactory

        def accessManager = accessManagerMock

        def accessMediator = accessMediatorMock
      }

      this.app = module

      tagManager = module.tagManager
    }

    override def tearDown(){
      app.stop()

      verify(app.accessManager)
      verify(app.accessMediator)

      Thread.sleep(500)

      assertTrue(ThreadWatcher.registeredThreads.isEmpty)
    }

    def testAddResource() {
       val initialMap = Map(("cats",2), ("scala",2), ("cycling", 2))
       val mapString = MetadataHelper.writeTagMap(initialMap, (key: String, value: Int) => {key + ":" + value})

       val map =  MetadataHelper.parseTagMap(mapString, (tagInfo: String) => {
                                     (tagInfo.split(":")(0), tagInfo.split(":")(1).toInt)
                                   }).toMap[String, Int]
       assertEquals(initialMap, map)
       tagManager ! ResourceAddedMsg(resource, Symbol("source"))
       Thread.sleep(1000)
       assertEquals(Some(MetadataHelper.writeTagMap(Map(("cats",1), ("scala",1),("cycling", 1)).toMap[String, Int], (key: String, value: Int) => {key + ":" + value})), parent.metadata(TagManager.TAGS_FIELD))
    }

    def testTagParsing() {
      val map: Map[String, Int] = Map(("cats",10), ("scala",1), ("cycling", 1))
      val mapString = MetadataHelper.writeTagMap(map, (key: String, value: Int) => {key + ":" + value})
      val parsedMap = MetadataHelper.parseTagMap(mapString, (tagInfo: String) => {
        println(tagInfo + " " +  (tagInfo.split(":")(0),tagInfo.split(":")(1).toInt))
        (tagInfo.split(":")(0), tagInfo.split(":")(1).toInt)
      }).toMap[String, Int]
      assertEquals(map, parsedMap)
    }

    def testUpdateResource() {
       tagManager ! ResourceUpdatedMsg(resource, Symbol("source"))
       Thread.sleep(1000)
       assertEquals(Some(MetadataHelper.writeTagMap(Map("scala" -> 1, "cycling" -> 1, "cats" -> 1).toMap[String, Int], (key: String, value: Int) => {key + ":" + value})), parent.metadata(TagManager.TAGS_FIELD))
    }

    def testDeleteResource() {
       tagManager.deleteResource(resource)
       Thread.sleep(1000)
       assertEquals(None, parent.metadata(TagManager.TAGS_FIELD))
    }
}
