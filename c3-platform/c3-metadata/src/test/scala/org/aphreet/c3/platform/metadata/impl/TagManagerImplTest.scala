package org.aphreet.c3.platform.metadata.impl

import junit.framework.TestCase
import org.easymock.EasyMock._
import org.aphreet.c3.platform.resource.{MetadataHelper, Metadata, Resource}
import org.aphreet.c3.platform.filesystem.{Directory, Node}
import org.aphreet.c3.platform.access.{AccessMediator, ResourceAddedMsg, AccessManager}
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
      val tagsInDeletedResource: Map[String, Int] = Map(("cats",1), ("scala",3), ("cycling", 2))

      val deletedTags: String = MetadataHelper.writeTagMap(tagsInDeletedResource)
      val metadata:Map[String, String] = Map((Resource.MD_TAGS, deletedTags))
      resource.metadata = new Metadata(new mutable.HashMap() ++= metadata)
      resource.address = "someAddress"
      resource.systemMetadata = new Metadata(new mutable.HashMap() ++=  Map((Node.NODE_FIELD_PARENT, parentPath)))
      val dir = Directory.emptyDirectory("someDir1", "")
      dir.addChild("dir1", resource.address, true)

      val dir2 = Directory.emptyDirectory("someDir2", "")
      parent.address = "parent1Address"
      dir2.addChild("dir2", parent.address, true)

      val dir3 = Directory.emptyDirectory("someDir3", "")
      parent2.address = "parent2Address"
      dir3.addChild("dir3", parent2.address, true)

      parent.metadata = new Metadata(new mutable.HashMap() ++= metadata)
      parent.systemMetadata = new Metadata(new mutable.HashMap() ++=  Map((Node.NODE_FIELD_PARENT, parent2Path)))

      parent2.metadata = new Metadata(new mutable.HashMap() ++= metadata)

      tagManager.accessManager = createMock(classOf[AccessManager])
      tagManager.accessMediator = createMock(classOf[AccessMediator])

      expect(tagManager.accessManager.update(parent)).andReturn("").anyTimes
      expect(tagManager.accessManager.update(parent2)).andReturn("").anyTimes
      expect(tagManager.accessManager.get(parentPath)).andReturn(parent).once()
      expect(tagManager.accessManager.get(parent2Path)).andReturn(parent2).once()


      replay(tagManager.accessManager)
      tagManager.init()
    }

    def testAddResource() {
       tagManager ! ResourceAddedMsg(resource, Symbol("source"))
       Thread.sleep(1000)
       assertEquals(Some(MetadataHelper.writeTagMap(Map(("cats",2), ("scala",6), ("cycling", 4)))), parent.metadata(Resource.MD_TAGS))
    }

    def testDeleteResource() {
       tagManager.deleteResource(resource)
       Thread.sleep(1000)
       assertEquals(Some(MetadataHelper.writeTagMap((Map.empty[String, Int]))), parent.metadata(Resource.MD_TAGS))
    }
}
