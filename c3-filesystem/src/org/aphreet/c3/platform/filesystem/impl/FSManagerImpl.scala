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

package org.aphreet.c3.platform.filesystem.impl

import org.aphreet.c3.platform.access.AccessManager
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.filesystem._
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.resource.{ResourceVersion, DataWrapper, Resource}

@Component
class FSManagerImpl extends FSManager{

  var accessManager:AccessManager = _

  @Autowired
  def setAccessManager(manager:AccessManager) = {accessManager = manager}

  def getNode(path:String):Node = {
    getFSNode(path)
  }

  def updateFile(path:String, data:DataWrapper, metadata:Map[String, String]) = {
    val node = getFSNode(path)

    if(node.isDirectory){
      throw new FSException("Can't update directory")
    }else{

      val version = new ResourceVersion
      version.data = data

      node.resource.systemMetadata ++= metadata

      node.resource.addVersion(version)

      accessManager.update(node.resource)
    }
  }

  def deleteNode(path:String) = {
    //getFSNode()
  }

  def createFile(path:String, name:String, resource:Resource) = {

    addNodeToDirectory(path, name, File.createFile(resource, name))
    
  }

  def createDirectory(path:String, name:String) = {

    addNodeToDirectory(path, name, Directory.emptyDirectory(name))

  }

  private def addNodeToDirectory(path:String, name:String, node:Node){

    val node = getFSNode(path)

    var directory:Directory = null

    if(node.isDirectory){
      directory = node.asInstanceOf[Directory]
    }else{
      throw new FSException("Can't add node to file")
    }


    try{
      accessManager.lock(directory.resource.address)

      //refreshing directory instance
      directory = Node.fromResource(accessManager.get(directory.resource.address)).asInstanceOf[Directory]

      val newAddress = accessManager.add(node.resource)

      directory.addChild(NodeRef(name, newAddress))

      accessManager.update(directory.resource)

    }catch{
      case e:StorageException =>
    }finally {
      accessManager.unlock(directory.resource.address)
    }
  }

  private def getFSNode(path:String):Node = {
    val pathComponents = getPathComponents(path)

    var resultNode:Node = getRoot

    for(directoryName <- pathComponents){

      if(!resultNode.isDirectory){
        throw new FSException("Found file, expected directory")
      }

      val address = resultNode.asInstanceOf[Directory].getChild(directoryName) match {
        case Some(a) => a
        case None => throw new FSException("Specified path does not exists")
      }

      val resource = accessManager.get(address)

      resultNode = Node.fromResource(resource)

    }

    resultNode

  }

  private def getPathComponents(path:String):Array[String] = {
    path.split("/")
  }

  private def getRoot:Directory = {
    null
  }
}