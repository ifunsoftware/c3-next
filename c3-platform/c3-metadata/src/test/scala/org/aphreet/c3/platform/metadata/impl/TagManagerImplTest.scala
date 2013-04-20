package org.aphreet.c3.platform.metadata.impl

import junit.framework.TestCase
import org.easymock.EasyMock._
import org.aphreet.c3.platform.resource.{MetadataHelper, Metadata, Resource}
import org.aphreet.c3.platform.filesystem.{Directory, Node}
import org.aphreet.c3.platform.access.{ResourceUpdatedMsg, AccessMediator, ResourceAddedMsg, AccessManager}
import collection.mutable
import junit.framework.Assert._

class TagManagerImplTest extends TestCase {

    var tagManager: TagManagerImpl = new TagManagerImpl
    val resource:Resource = new Resource()
    val parent:Resource = new Resource()
    val parent2:Resource = new Resource()
    val parentPath:String = "parentPath"
    val parent2Path:String = "parent2Path"

    override def setUp() {
      val resourceMetadata:Map[String, String] = Map((Resource.MD_TAGS, "[cats,scala,cycling]"))

      val tagsInResource: Map[String, Int] = Map(("cats",1), ("scala",1), ("cycling", 1))
      val deletedTags: String = MetadataHelper.writeTagMap(tagsInResource.toMap[String, Int], (key: String, value: Int) => {key + ":" + value})

      val dirMetadata:Map[String, String] = Map((Resource.MD_TAGS, deletedTags))

      resource.metadata = new Metadata(new mutable.HashMap() ++= resourceMetadata)
      resource.address = "someAddress"
      resource.systemMetadata = new Metadata(new mutable.HashMap() ++=  Map((Node.NODE_FIELD_PARENT, parentPath),(Node.NODE_FIELD_TYPE, "file")))

      parent.metadata = new Metadata(new mutable.HashMap() ++= dirMetadata)
      parent.systemMetadata = new Metadata(new mutable.HashMap() ++=  Map((Node.NODE_FIELD_PARENT, parent2Path),(Node.NODE_FIELD_TYPE, "directory")))
      parent.address = "parent1Address"

      parent2.metadata = new Metadata(new mutable.HashMap() ++= dirMetadata)
      parent2.systemMetadata = new Metadata(new mutable.HashMap() ++=  Map((Node.NODE_FIELD_TYPE, "directory")))
      parent2.address = "parent2Address"

      val dir2 = Directory.emptyDirectory("someDir2", "")
      dir2.addChild("dir2", parent2.address, true)

      val parent2Node = Node.fromResource(parent2)
      val parent2Dir = parent2Node.asInstanceOf[Directory]
      parent2Dir.addChild("dir1", parent.address, true)

      val parentNode = Node.fromResource(parent)
      val parentDir = parentNode.asInstanceOf[Directory]
      parentDir.addChild("file", resource.address, true)

      tagManager.accessManager = createMock(classOf[AccessManager])
      tagManager.accessMediator = createMock(classOf[AccessMediator])

      expect(tagManager.accessManager.get(resource.address)).andReturn(resource).anyTimes
      expect(tagManager.accessManager.update(parent)).andReturn("").anyTimes
      expect(tagManager.accessManager.update(parent2)).andReturn("").anyTimes
      expect(tagManager.accessManager.get(parentPath)).andReturn(parent).anyTimes()
      expect(tagManager.accessManager.get(parent2Path)).andReturn(parent2).anyTimes()

      replay(tagManager.accessManager)
      tagManager.init()
    }

    def testAddResource() {
       tagManager ! ResourceAddedMsg(resource, Symbol("source"))
       Thread.sleep(1000)
       assertEquals(Some(MetadataHelper.writeTagMap(Map(("cats",2), ("scala",2), ("cycling", 2)).toMap[String, Int], (key: String, value: Int) => {key + ":" + value})), parent.metadata(Resource.MD_TAGS))
    }

    def testUpdateResource() {
       tagManager ! ResourceUpdatedMsg(resource, Symbol("source"))
       Thread.sleep(1000)
       assertEquals(Some(MetadataHelper.writeTagMap(Map(("cats",1), ("scala",1), ("cycling", 1)).toMap[String, Int], (key: String, value: Int) => {key + ":" + value})), parent.metadata(Resource.MD_TAGS))
    }

    def testDeleteResource() {
       tagManager.deleteResource(resource)
       Thread.sleep(1000)
       assertEquals(Some(""), parent.metadata(Resource.MD_TAGS))
    }
}
