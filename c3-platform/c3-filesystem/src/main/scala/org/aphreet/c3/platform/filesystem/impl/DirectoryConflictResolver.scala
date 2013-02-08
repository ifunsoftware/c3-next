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

    filterUpdatedDeletedNodes(resultNodes)

    savedDirectory.importChildren(resultNodes)
  }

  private def filterUpdatedDeletedNodes(resultNodes: mutable.HashMap[String, NodeRef]){

    val deletedAddresses = resultNodes.filter(kv => kv._2.deleted == true).map(kv => kv._2.address).toSet

    val deletedNodes = resultNodes.filter(kv => deletedAddresses.contains(kv._2.address))

    for ((name, ref) <- deletedNodes){
      resultNodes.put(name, NodeRef(name, ref.address, ref.leaf, deleted = true, ref.modified))
    }
  }

  private def addAllNodes(resultNodes: mutable.HashMap[String, NodeRef], directory: Directory){
    for(node <- directory.getChildren){
      resultNodes.get(node.name) match {
        case Some(nodeRef) => {

          if (!nodeRef.deleted){
            if (node.deleted){
              resultNodes.put(node.name, node)
            }else{
              if (node.address == nodeRef.address){
                if (node.modified > nodeRef.modified){
                  resultNodes.put(node.name, node)
                }
              }else{
                //We have two files or directories with the same name but with different content
                resultNodes.remove(node.name)
                resultNodes.put(node.name + "-" + node.modified, NodeRef(node.name + "-" + node.modified, node.address, node.leaf, false, node.modified))
                resultNodes.put(nodeRef.name + "-" + nodeRef.modified, NodeRef(nodeRef.name + "-" + nodeRef.modified, nodeRef.address, nodeRef.leaf, false, nodeRef.modified))
              }
            }
          }
        }
        case None => resultNodes.put(node.name, node)
      }
    }
  }

}
