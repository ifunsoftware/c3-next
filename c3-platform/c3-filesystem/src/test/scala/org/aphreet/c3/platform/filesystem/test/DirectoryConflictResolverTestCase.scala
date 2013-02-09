package org.aphreet.c3.platform.filesystem.test

import junit.framework.Assert._
import junit.framework.TestCase
import org.aphreet.c3.platform.filesystem.{NodeRef, Node, Directory}
import org.aphreet.c3.platform.filesystem.impl.DirectoryConflictResolver


class DirectoryConflictResolverTestCase extends TestCase{

  def testTrivialDirectoryMerge(){

    val initialDirectory = Directory.emptyDirectory("domain0", "name")
    initialDirectory.addChild("name0", "aaaaaaaa", leaf = true)
    initialDirectory.addChild("name1", "aaaabbbb", leaf = true)

    val resource = initialDirectory.resource

    Thread.sleep(10)

    val directory0 = Node.fromResource(resource.clone).asInstanceOf[Directory]
    directory0.addChild("name2", "aaaacccc", leaf=true)

    Thread.sleep(10)

    val directory1 = Node.fromResource(resource.clone).asInstanceOf[Directory]
    directory1.addChild("name3", "aaaadddd", leaf=true)

    val resource0 = directory0.resource
    val resource1 = directory1.resource

    new DirectoryConflictResolver().resolve(resource0, resource1)

    val mergedDirectory = Node.fromResource(resource0).asInstanceOf[Directory]

    val children = mergedDirectory.getChildren

    assertEquals(4, children.length)

    compareRefsWithoutTime(NodeRef("name0", "aaaaaaaa", leaf = true, deleted = false, 0L), children(0))
    compareRefsWithoutTime(NodeRef("name1", "aaaabbbb", leaf = true, deleted = false, 0L), children(1))
    compareRefsWithoutTime(NodeRef("name2", "aaaacccc", leaf = true, deleted = false, 0L), children(2))
    compareRefsWithoutTime(NodeRef("name3", "aaaadddd", leaf = true, deleted = false, 0L), children(3))

  }

  def testNameCollisionDirectoryMerge(){

    val initialDirectory = Directory.emptyDirectory("domain0", "name")
    initialDirectory.addChild("name0", "aaaaaaaa", leaf = true)
    initialDirectory.addChild("name1", "aaaabbbb", leaf = true)

    val resource = initialDirectory.resource

    Thread.sleep(10)

    val directory0 = Node.fromResource(resource.clone).asInstanceOf[Directory]
    directory0.addChild("name2", "aaaaeeee", leaf=true)

    Thread.sleep(10)

    val directory1 = Node.fromResource(resource.clone).asInstanceOf[Directory]
    directory1.addChild("name2", "aaaadddd", leaf=true)

    val resource0 = directory0.resource
    val resource1 = directory1.resource

    new DirectoryConflictResolver().resolve(resource0, resource1)

    val mergedDirectory = Node.fromResource(resource0).asInstanceOf[Directory]

    val children = mergedDirectory.getChildren

    assertEquals(4, children.length)

    compareRefsWithoutTime(NodeRef("name0", "aaaaaaaa", leaf = true, deleted = false, 0L), children(0))
    compareRefsWithoutTime(NodeRef("name1", "aaaabbbb", leaf = true, deleted = false, 0L), children(1))
    compareRefsWithoutTime(NodeRef("name2" + "-" + children(2).modified, "aaaaeeee", leaf = true, deleted = false, 0L), children(2))
    compareRefsWithoutTime(NodeRef("name2" + "-" + children(3).modified, "aaaadddd", leaf = true, deleted = false, 0L), children(3))

  }

  def testDeletedCollisionDirectoryMerge(){

    val initialDirectory = Directory.emptyDirectory("domain0", "name")
    initialDirectory.addChild("name0", "aaaaaaaa", leaf = true)
    initialDirectory.addChild("name1", "aaaabbbb", leaf = true)

    val resource = initialDirectory.resource

    Thread.sleep(10)

    val directory0 = Node.fromResource(resource.clone).asInstanceOf[Directory]
    directory0.updateChild("name1", "name2")

    Thread.sleep(10)

    val directory1 = Node.fromResource(resource.clone).asInstanceOf[Directory]
    directory1.removeChild("name1")

    val resource0 = directory0.resource
    val resource1 = directory1.resource

    new DirectoryConflictResolver().resolve(resource0, resource1)

    val mergedDirectory = Node.fromResource(resource0).asInstanceOf[Directory]

    val children = mergedDirectory.getChildren

    assertEquals(3, children.length)

    compareRefsWithoutTime(NodeRef("name0", "aaaaaaaa", leaf = true, deleted = false, 0L), children(0))
    compareRefsWithoutTime(NodeRef("name1", "aaaabbbb", leaf = true, deleted = true, 0L), children(1))
    compareRefsWithoutTime(NodeRef("name2", "aaaabbbb", leaf = true, deleted = true, 0L), children(2))
  }

  private def compareRefsWithoutTime(nodeRef1: NodeRef, nodeRef2: NodeRef){
    assertEquals(nodeRef1.name, nodeRef2.name)
    assertEquals(nodeRef1.address, nodeRef2.address)
    assertEquals(nodeRef1.leaf, nodeRef2.leaf)
    assertEquals(nodeRef1.deleted, nodeRef2.deleted)
  }

}
