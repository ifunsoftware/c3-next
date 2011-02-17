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
import org.aphreet.c3.platform.resource.{DataWrapper, ResourceVersion, Resource}
import java.io.{DataInputStream, ByteArrayInputStream, DataOutputStream, ByteArrayOutputStream}

abstract class Node(val resource:Resource){

  def isDirectory:Boolean

}


object Node{

  val NODE_NAME_FIELD = "c3.fs.nodename"

  val NODE_TYPE_FIELD = "c3.fs.nodetype"

  val NODE_PARENT_FIELD = "c3.fs.parent"

  val NODE_TYPE_FILE = "file"

  val NODE_TYPE_DIR = "directory"

  val DIRECTORY_CONTENT_TYPE = "application/x-c3-directory"

  def fromResource(resource:Resource):Node = {
    resource.systemMetadata.get(NODE_TYPE_FIELD) match {
      case Some(value) => value match {
        case "directory" => Directory(resource)
        case "file" => File(resource)
        case _ => throw new FSException("Referenced node has unknown type")
      }
      case None => throw new FSException("Referenced node does not have a type")
    }
  }
}

case class NodeRef(val name:String, val address:String)

case class File(override val resource:Resource) extends Node(resource){

  override def isDirectory = false

}

object File{

  def createFile(resource:Resource, name:String):File = {
    resource.systemMetadata.put(Node.NODE_TYPE_FIELD, Node.NODE_TYPE_FILE)
    resource.systemMetadata.put(Node.NODE_NAME_FIELD, name)

    File(resource)
  }
}

case class Directory(override val resource:Resource) extends Node(resource){

  private val children = new HashMap[String, String]

  {
    readData
  }

  override def isDirectory = true

  def getChild(name:String):Option[String] = {
    children.get(name)
  }

  def addChild(node:NodeRef) = {
    children.put(node.name, node.address)

    updateResource
  }

  def removeChild(name:String) = {
    children.remove(name)
    updateResource
  }

  def getChildren:Array[NodeRef] = {
    children.map(e => NodeRef(e._1, e._2)).toArray
  }


  protected def updateResource = {
    val version = new ResourceVersion
    version.data = getData(children)

  }

  private def getData(children:HashMap[String, String]):DataWrapper = {

    val byteOs = new ByteArrayOutputStream
    val dataOs = new DataOutputStream(byteOs)

    dataOs.writeShort(0)
    dataOs.writeInt(children.size)

    for((name, address) <- children){
      dataOs.writeUTF(address)
      dataOs.writeUTF(name)
    }

    DataWrapper.wrap(byteOs.toByteArray)
  }

  private def readData = {

    if(resource.versions.length > 0){

      val byteIn = new ByteArrayInputStream(resource.versions.last.data.getBytes)

      val dataIn = new DataInputStream(byteIn)

      dataIn.readShort

      val count = dataIn.readInt

      for(i <- 1 to count){
        val address = dataIn.readUTF
        val name = dataIn.readUTF
        children.put(name, address)
      }
    }
  }
}

object Directory{

  def emptyDirectory(name:String):Directory = {
    val resource = new Resource
    resource.systemMetadata.put(Node.NODE_TYPE_FIELD, Node.NODE_TYPE_DIR)

    if(name != null){
      resource.systemMetadata.put(Node.NODE_NAME_FIELD, name)
    }

    resource.metadata.put(Resource.MD_CONTENT_TYPE, Node.DIRECTORY_CONTENT_TYPE)
    resource.systemMetadata.put(Resource.MD_CONTENT_TYPE, Node.DIRECTORY_CONTENT_TYPE)

    resource.isVersioned = false


    val directory = Directory(resource)

    directory.updateResource

    directory
  }
}

