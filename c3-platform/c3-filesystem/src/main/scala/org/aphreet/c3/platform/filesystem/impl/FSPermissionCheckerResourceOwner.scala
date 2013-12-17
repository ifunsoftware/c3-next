package org.aphreet.c3.platform.filesystem.impl

import org.aphreet.c3.platform.access.ResourceOwner
import org.aphreet.c3.platform.filesystem.{Directory, Node}
import org.aphreet.c3.platform.resource.Resource

/**
 * Author: Mikhail Malygin
 * Date:   12/16/13
 * Time:   6:08 PM
 */
trait FSPermissionCheckerResourceOwner extends ResourceOwner {

  def fsRoots: Map[String, String]

  override def resourceCanBeDeleted(resource: Resource): Boolean = {
    resource.systemMetadata(Node.NODE_FIELD_TYPE) match {
      case None => true
      case Some(nodeType) => {

        var canDelete = false

        if (nodeType == Node.NODE_TYPE_DIR) {
          canDelete = fsRoots.values.forall(_ != resource.address)
        } else {
          canDelete = true
        }

        canDelete
      }
    }
  }

  override def resourceCanBeUpdated(resource: Resource): Boolean = {
    resource.systemMetadata(Node.NODE_FIELD_TYPE) match {
      case None => true
      case Some(nodeType) =>
        if (nodeType == Node.NODE_TYPE_DIR) {
          try {
            //Let's consider that we can't have directories with size more than 10MB
            //As max file name limited to 512 chars, it is enough to have more than 10000 files in
            // a directory
            if (resource.versions.last.data.length > 10L * 1024 * 1024) {
              false
            } else {
              //trying to create a directory from provided ByteStream
              Directory(resource)
              true
            }
          } catch {
            case e: Throwable => false
          }
        } else {
          true
        }
    }
  }
}
