package org.aphreet.c3.platform.filesystem.impl

import org.aphreet.c3.platform.storage.ConflictResolver
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.filesystem.{NodeRef, Node, Directory}
import scala.collection.mutable
import org.aphreet.c3.platform.common.Logger

class DirectoryConflictResolver extends ConflictResolver{

  val log = Logger(getClass)

  def resolve(savedResource: Resource, incomeResource: Resource) {

    log.info("Resolving conflict: saved({}, {}), income({}, {})", Array(
      savedResource.versions.last.date.getTime, savedResource.versions.last.basedOnVersion,
      incomeResource.versions.last.date.getTime, incomeResource.versions.last.basedOnVersion))

    val resultNodes = new mutable.HashMap[String, NodeRef]()

    val savedDirectory = Node.fromResource(savedResource).asInstanceOf[Directory]
    val incomeDirectory = Node.fromResource(incomeResource).asInstanceOf[Directory]

    addAllNodes(resultNodes, savedDirectory)
    addAllNodes(resultNodes, incomeDirectory)

    filterUpdatedDeletedNodes(resultNodes)

    savedDirectory.importChildren(resultNodes)
    savedResource.calculateCheckSums
  }

  private def filterUpdatedDeletedNodes(resultNodes: mutable.HashMap[String, NodeRef]){

    val deletedAddresses: collection.Map[String, NodeRef] = resultNodes.filter{case (name, ref) => ref.deleted}.map{case (name, ref) => (ref.address, ref)}

    if(!deletedAddresses.isEmpty){
      val deletedNodes = resultNodes.filter{case (name, ref) => {
        deletedAddresses.get(ref.address) match {
          case Some(foundRef) => foundRef.modified > ref.modified
          case None => false
        }
      }}

      for ((name, ref) <- deletedNodes){
        resultNodes.put(name, NodeRef(name, ref.address, ref.leaf, deleted = true, ref.modified))
      }
    }
  }

  private def addAllNodes(resultNodes: mutable.HashMap[String, NodeRef], directory: Directory){
    for(node <- directory.allChildren){
      resultNodes.get(node.name) match {
        case Some(nodeRef) => {


          if(node.address == nodeRef.address){
            if (node.modified > nodeRef.modified){
              resultNodes.put(node.name, node)
            }
          }else{
            //We have two files or directories with the same name but with different content
            resultNodes.remove(node.name)
            resultNodes.put(node.name + "-" + node.modified, NodeRef(node.name + "-" + node.modified, node.address, node.leaf, node.deleted, node.modified))
            resultNodes.put(nodeRef.name + "-" + nodeRef.modified, NodeRef(nodeRef.name + "-" + nodeRef.modified, nodeRef.address, nodeRef.leaf, nodeRef.deleted, nodeRef.modified))
          }
        }
        case None => resultNodes.put(node.name, node)
      }
    }
  }

}
