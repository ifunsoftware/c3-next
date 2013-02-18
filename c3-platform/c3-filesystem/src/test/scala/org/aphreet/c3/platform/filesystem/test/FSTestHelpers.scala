package org.aphreet.c3.platform.filesystem.test

import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.filesystem.{File, Directory, Node}

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */
trait FSTestHelpers {

  def resourceStub(name:String, parentAddress:String):Resource = {
    val resource = new Resource

    if(name != null){
      resource.systemMetadata.put(Node.NODE_FIELD_NAME, name)
    }

    if(parentAddress != null){
      resource.systemMetadata.put(Node.NODE_FIELD_PARENT, parentAddress)
    }

    resource
  }

  def directoryStub(res: Resource): Directory = {
    res.systemMetadata.put(Node.NODE_FIELD_TYPE, "directory")
    Node.fromResource(res).asInstanceOf[Directory]
  }

  def fileStub(res: Resource): File = {
    res.systemMetadata.put(Node.NODE_FIELD_TYPE, "file")
    Node.fromResource(res).asInstanceOf[File]
  }
}