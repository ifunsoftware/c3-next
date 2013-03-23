/*
 * Copyright (c) 2012, Mikhail Malygin
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

import org.aphreet.c3.platform.resource.{DataStream, ResourceVersion, Resource}
import java.io.{DataInputStream, ByteArrayInputStream, DataOutputStream, ByteArrayOutputStream}
import collection.immutable.TreeMap
import java.util.{UUID, Date}
import scala.collection.mutable
import org.aphreet.c3.platform.common.Logger

abstract class Node(val resource:Resource){

  def isDirectory:Boolean

  def name:String = resource.systemMetadata.asMap.getOrElse(Node.NODE_FIELD_NAME, null)
}


object Node{

  val log = Logger(classOf[Node])

  val NODE_FIELD_NAME = "c3.fs.nodename"

  val NODE_FIELD_TYPE = "c3.fs.nodetype"

  val NODE_FIELD_PARENT = "c3.fs.parent"

  val NODE_TYPE_FILE = "file"

  val NODE_TYPE_DIR = "directory"

  val DIRECTORY_CONTENT_TYPE = "application/x-c3-directory"

  def fromResource(resource:Resource):Node = {
    resource.systemMetadata(NODE_FIELD_TYPE) match {
      case Some(value) => value match {
        case "directory" => Directory(resource)
        case "file" => File(resource)
        case _ => throw new FSException("Referenced node has unknown type")
      }
      case None => throw new FSException("Referenced node does not have a type")
    }
  }

  def canBuildFromResource(resource:Resource):Boolean = {
    resource.systemMetadata(NODE_FIELD_TYPE) match {
      case Some(value) => value match {
        case "directory" => true
        case "file" => true
        case _ => false
      }
      case None => false
    }
  }
}

case class NodeRef(name:String, address:String, leaf:Boolean, deleted: Boolean, modified: Long){

  def delete(timestamp: Long) :NodeRef = NodeRef(name, address, leaf, deleted = true, timestamp)

  def update(newName: String, timestamp: Long): NodeRef = NodeRef(newName, address, leaf, deleted = false, timestamp)

}

case class File(override val resource:Resource) extends Node(resource){

  override def isDirectory = false

}

object File{

  def createFile(resource:Resource, domainId:String, name:String):File = {
    resource.systemMetadata(Node.NODE_FIELD_TYPE) = Node.NODE_TYPE_FILE
    resource.systemMetadata(Node.NODE_FIELD_NAME) = name
    resource.systemMetadata("c3.domain.id") = domainId

    File(resource)
  }
}

case class Directory(override val resource:Resource) extends Node(resource){

  private var childrenMap = new TreeMap[String, NodeRef]

  private var persistedVersionTimestamp = 0L

  private var updateTimestamp = -1L

  private var randomUUID: UUID = null

  {
    readData()
  }

  override def isDirectory = true

  def getChild(name:String):Option[NodeRef] = {

    Node.log debug childrenMap.toString

    childrenMap.get(name)
  }

  def addChild(name: String, address: String, leaf: Boolean) {

    childrenMap += (name -> NodeRef(name, address, leaf, deleted = false, modified = takeUpdateTimestamp()))

    updateResource()
  }

  def removeChild(name:String) {

    childrenMap.get(name) match {
      case Some(nodeRef) => {
        childrenMap += (nodeRef.name -> nodeRef.delete(takeUpdateTimestamp()))
      }
      case None =>
    }

    updateResource()
  }

  def updateChild(name: String, newName: String) {
    childrenMap.get(name) match {
      case Some(nodeRef) => {
        childrenMap += (name -> nodeRef.delete(takeUpdateTimestamp()))
        childrenMap += (newName -> nodeRef.update(newName, takeUpdateTimestamp()))
      }
      case None =>
    }

    updateResource()
  }

  def allChildren:Array[NodeRef] = {
    childrenMap.values.toArray
  }

  def children:Array[NodeRef] = {
    childrenMap.values.filter(!_.deleted).toArray
  }

  def importChildren(newChildren: mutable.Map[String, NodeRef]){
    childrenMap = new TreeMap[String, NodeRef]() ++ newChildren
    updateResource()
  }

  protected def updateResource() {
    val version = new ResourceVersion
    version.data = writeData(childrenMap)
    version.persisted = false
    resource.addVersion(version)
    version.date = new Date(takeUpdateTimestamp())

    //As this method can be called several times before resource
    //actually will be stored
    //make sure, that created version references
    //pervious persisted version
    version.basedOnVersion = persistedVersionTimestamp
  }

  protected def takeUpdateTimestamp(): Long = {
    if(updateTimestamp == -1L){
      updateTimestamp = System.currentTimeMillis()
    }

    updateTimestamp
  }

  private def writeData(children:Map[String, NodeRef]):DataStream = {

    def isOldEntry(entry: NodeRef, ts: Long): Boolean = {
      entry.deleted && ts - entry.modified > 24 * 60 * 60 * 10 * 1000L
    }


    val byteOs = new ByteArrayOutputStream
    val dataOs = new DataOutputStream(byteOs)

    dataOs.writeShort(1)

    val ts = System.currentTimeMillis()

    val filterdChildren = children
      .filter(c => !isOldEntry(c._2, ts))

    dataOs.writeInt(filterdChildren.size)

    for((name, nodeRef) <- filterdChildren){
      dataOs.writeUTF(nodeRef.address)
      dataOs.writeBoolean(nodeRef.leaf)
      dataOs.writeUTF(name)
      dataOs.writeBoolean(nodeRef.deleted)
      dataOs.writeLong(nodeRef.modified)
    }

    //We just need some entropy to make sure that initial directory content
    //is not the same -> content md5 is not the same
    //and as a result storage is not the same
    dataOs.writeLong(resource.createDate.getTime)

    if(randomUUID != null){
      dataOs.writeLong(randomUUID.getLeastSignificantBits)
      dataOs.writeLong(randomUUID.getMostSignificantBits)
    }

    DataStream.create(byteOs.toByteArray)
  }

  private def readData() {

    if(resource.versions.length > 0){

      val version = resource.versions.last

      persistedVersionTimestamp = version.date.getTime

      val byteIn = new ByteArrayInputStream(version.data.getBytes)

      val dataIn = new DataInputStream(byteIn)

      val serializationFormat = dataIn.readShort

      val count = dataIn.readInt

      for(i <- 1 to count){
        val address = dataIn.readUTF
        val leaf = dataIn.readBoolean
        val name = dataIn.readUTF

        if(serializationFormat > 0){

          val deleted = dataIn.readBoolean()
          val modified = dataIn.readLong()

          childrenMap += (name -> NodeRef(name, address, leaf, deleted, modified))

        }else{
          childrenMap += (name -> NodeRef(name, address, leaf, deleted = false, modified = version.date.getTime))
        }
      }
    }
  }
}

object Directory{

  def emptyDirectory(domainId:String, name:String, meta: Map[String, String] = Map()):Directory = {
    val resource = new Resource
    resource.isVersioned = false
    resource.systemMetadata(Node.NODE_FIELD_TYPE) = Node.NODE_TYPE_DIR

    if(name != null){
      resource.systemMetadata(Node.NODE_FIELD_NAME) = name
    }

    resource.systemMetadata("c3.skip.index") = "true"
    resource.systemMetadata("c3.domain.id") = domainId

    resource.metadata(Resource.MD_CONTENT_TYPE) = Node.DIRECTORY_CONTENT_TYPE
    resource.systemMetadata(Resource.MD_CONTENT_TYPE) = Node.DIRECTORY_CONTENT_TYPE

    meta foreach { case (k,v) => resource.metadata(k) = v }

    val directory = Directory(resource)

    directory.randomUUID = UUID.randomUUID()

    directory.updateResource()

    directory
  }
}

