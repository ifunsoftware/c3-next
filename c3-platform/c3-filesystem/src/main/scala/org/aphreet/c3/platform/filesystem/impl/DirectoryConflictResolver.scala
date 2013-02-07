package org.aphreet.c3.platform.filesystem.impl

import org.aphreet.c3.platform.storage.ConflictResolver
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.filesystem.{NodeRef, Node, Directory}
import scala.collection.mutable

class DirectoryConflictResolver extends ConflictResolver{

  def resolve(savedResource: Resource, incomeResource: Resource) {

    val resultNodes = new mutable.HashMap[String, NodeRef]()

    val savedDirectory = Node.fromResource(savedResource).asInstanceOf[Directory]
    val incomeDirectory = Node.fromResource(incomeResource).asInstanceOf[Directory]

    addAllNodes(resultNodes, savedDirectory)
    addAllNodes(resultNodes, incomeDirectory)

    savedDirectory.importChildren(resultNodes)
  }

  private def addAllNodes(resultNodes: mutable.HashMap[String, NodeRef], directory: Directory){
    for(node <- directory.getChildren){
      resultNodes.get(node.name) match {
        case Some(nodeRef) => {
          if (node.modified > nodeRef.modified){
            resultNodes.put(node.name, node)
          }
        }
        case None => resultNodes.put(node.name, node)
      }
    }
  }

}
