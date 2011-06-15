/**
 * Copyright (c) 2011, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *

 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.platform.filesystem

import collection.mutable.HashMap
import org.aphreet.c3.platform.resource.{DataStream, ResourceVersion, Resource}
import java.io.{DataInputStream, ByteArrayInputStream, DataOutputStream, ByteArrayOutputStream}
import org.apache.commons.logging.LogFactory

abstract class Node(val resource:Resource){

  def isDirectory:Boolean

}


object Node{

  val log = LogFactory.getLog(classOf[Node])

  val NODE_FIELD_NAME = "c3.fs.nodename"

  val NODE_FIELD_TYPE = "c3.fs.nodetype"

  val NODE_FIELD_PARENT = "c3.fs.parent"

  val NODE_TYPE_FILE = "file"

  val NODE_TYPE_DIR = "directory"

  val DIRECTORY_CONTENT_TYPE = "application/x-c3-directory"

  def fromResource(resource:Resource):Node = {
    resource.systemMetadata.get(NODE_FIELD_TYPE) match {
      case Some(value) => value match {
        case "directory" => Directory(resource)
        case "file" => File(resource)
        case _ => throw new FSException("Referenced node has unknown type")
      }
      case None => throw new FSException("Referenced node does not have a type")
    }
  }

  def canBuildFromResource(resource:Resource):Boolean = {
    resource.systemMetadata.get(NODE_FIELD_TYPE) match {
      case Some(value) => value match {
        case "directory" => true
        case "file" => true
        case _ => false
      }
      case None => false
    }
  }
}

case class NodeRef(val name:String, val address:String, val leaf:Boolean)

case class File(override val resource:Resource) extends Node(resource){

  override def isDirectory = false

}

object File{

  def createFile(resource:Resource, domainId:String, name:String):File = {
    resource.systemMetadata.put(Node.NODE_FIELD_TYPE, Node.NODE_TYPE_FILE)
    resource.systemMetadata.put(Node.NODE_FIELD_NAME, name)
    resource.systemMetadata.put("c3.domain.id", domainId)

    File(resource)
  }
}

case class Directory(override val resource:Resource) extends Node(resource){

  private val children = new HashMap[String, NodeRef]

  {
    readData
  }

  override def isDirectory = true

  def getChild(name:String):Option[NodeRef] = {

    Node.log debug children.toString

    children.get(name)
  }

  def addChild(node:NodeRef) = {
    children.put(node.name, node)

    updateResource
  }

  def removeChild(name:String) = {
    children.remove(name)
    updateResource
  }

  def getChildren:Array[NodeRef] = {
    children.values.toArray
  }


  protected def updateResource = {
    val version = new ResourceVersion
    version.data = getData(children)
    version.persisted = false
    resource.addVersion(version)
  }

  private def getData(children:HashMap[String, NodeRef]):DataStream = {

    val byteOs = new ByteArrayOutputStream
    val dataOs = new DataOutputStream(byteOs)

    dataOs.writeShort(0)
    dataOs.writeInt(children.size)

    for((name, nodeRef) <- children){
      dataOs.writeUTF(nodeRef.address)
      dataOs.writeBoolean(nodeRef.leaf)
      dataOs.writeUTF(name)
    }

    DataStream.create(byteOs.toByteArray)
  }

  private def readData = {

    if(resource.versions.length > 0){

      val byteIn = new ByteArrayInputStream(resource.versions.last.data.getBytes)

      val dataIn = new DataInputStream(byteIn)

      dataIn.readShort

      val count = dataIn.readInt

      for(i <- 1 to count){
        val address = dataIn.readUTF
        val leaf = dataIn.readBoolean
        val name = dataIn.readUTF
        children.put(name, NodeRef(name, address, leaf))
      }
    }
  }
}

object Directory{

  def emptyDirectory(domainId:String, name:String):Directory = {
    val resource = new Resource
    resource.systemMetadata.put(Node.NODE_FIELD_TYPE, Node.NODE_TYPE_DIR)

    if(name != null){
      resource.systemMetadata.put(Node.NODE_FIELD_NAME, name)
    }

    resource.systemMetadata.put("c3.skip.index", "true")
    resource.systemMetadata.put("c3.domain.id", domainId)

    resource.metadata.put(Resource.MD_CONTENT_TYPE, Node.DIRECTORY_CONTENT_TYPE)
    resource.systemMetadata.put(Resource.MD_CONTENT_TYPE, Node.DIRECTORY_CONTENT_TYPE)

    val directory = Directory(resource)

    directory.updateResource

    directory
  }
}

