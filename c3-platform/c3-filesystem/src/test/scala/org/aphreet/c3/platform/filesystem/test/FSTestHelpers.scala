package org.aphreet.c3.platform.filesystem.test

import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.filesystem.{File, Directory, Node}

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */
trait FSTestHelpers {

  def resourceStub(name:String, parentAddress:String, address: String = null):Resource = {
    val resource = new Resource

    if(name != null){
      resource.systemMetadata(Node.NODE_FIELD_NAME) = name
    }

    if(parentAddress != null){
      resource.systemMetadata(Node.NODE_FIELD_PARENT) = parentAddress
    }

    if (address != null)
      resource.address = address

    resource
  }

  def directoryStub(res: Resource): Directory = {
    res.systemMetadata(Node.NODE_FIELD_TYPE) = "directory"
    Node.fromResource(res).asInstanceOf[Directory]
  }

  def fileStub(res: Resource): File = {
    res.systemMetadata(Node.NODE_FIELD_TYPE) = "file"
    Node.fromResource(res).asInstanceOf[File]
  }
}