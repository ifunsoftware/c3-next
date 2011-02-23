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
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.resource.{ResourceVersion, DataWrapper, Resource}
import org.aphreet.c3.platform.filesystem._
import javax.annotation.PostConstruct
import org.apache.commons.logging.LogFactory

@Component("fsManager")
class FSManagerImpl extends FSManager{

  val log = LogFactory getLog getClass

  var accessManager:AccessManager = _

  var configAccessor:FSConfigAccessor = _

  var rootAddress:String = null

  @Autowired
  def setAccessManager(manager:AccessManager) = {accessManager = manager}

  @Autowired
  def setConfigAccessor(accessor:FSConfigAccessor) = {configAccessor = accessor}

  @PostConstruct
  def init{

    log info "Starting Filesystem manager"

    val map = configAccessor.load

    map.get(FSConfigAccessor.ROOT_ADDRESS) match{
      case Some(address) => {
        rootAddress = address
        log info "Found filesystem root address"
      }
      case None => {
        log.warn("Filesystem root address is not found")
      }
    }
  }

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

    val components = getPathComponents(path)

    val nodeToDelete = components.lastOption match{
      case Some(x) => x
      case None => throw new FSException("Can't remove root node")
    }

    val parentPath = path.replaceFirst(nodeToDelete + "$", "")


    val node = getNode(path)

    if(node.isDirectory){
      val directory = node.asInstanceOf[Directory]
      if(!directory.getChildren.isEmpty){
        throw new FSException("Can't remove non-empty directory")
      }
    }

    val parentNode = getNode(parentPath).asInstanceOf[Directory]

    parentNode.removeChild(nodeToDelete)

    accessManager.update(parentNode.resource)
    accessManager.delete(node.resource.address)
  }

  def createFile(fullPath:String, resource:Resource) = {

    val pathAndName = splitPath(fullPath)

    val name = pathAndName._1
    val path = pathAndName._2

    if(log.isDebugEnabled){
      log.debug("Creating file " + name + " at path " + path)
    }

    addNodeToDirectory(path, name, File.createFile(resource, name))

  }

  def createDirectory(fullPath:String) = {

    val pathAndName = splitPath(fullPath)

    val name = pathAndName._1
    val path = pathAndName._2

    if(log.isDebugEnabled){
      log.debug("Creating directory " + name + " at path " + path)
    }

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

      node.resource.systemMetadata.put(Node.NODE_PARENT_FIELD, directory.resource.address)

      val newAddress = accessManager.add(node.resource)

      directory.addChild(NodeRef(name, newAddress, !node.isDirectory))

      accessManager.update(directory.resource)

    }catch{
      case e:StorageException =>
    }finally {
      accessManager.unlock(directory.resource.address)
    }
  }

  private def getFSNode(path:String):Node = {

    if(log.isDebugEnabled){
      log.debug("Looking for node for path: " + path)
    }

    val pathComponents = getPathComponents(path).filter(_.length > 0)

    var resultNode:Node = getRoot

    for(directoryName <- pathComponents){

      if(log.isTraceEnabled){
        log.trace("Getting node: " + directoryName)
      }

      if(!resultNode.isDirectory){
        throw new FSException("Found file, expected directory")
      }

      val nodeRef = resultNode.asInstanceOf[Directory].getChild(directoryName) match {
        case Some(a) => a
        case None => throw new FSException("Specified path does not exists")
      }

      val resource = accessManager.get(nodeRef.address)

      resultNode = Node.fromResource(resource)

    }

    if(log.isDebugEnabled){
      log.debug("Found node for path: " + path)
    }

    resultNode

  }

  private def getPathComponents(path:String):Array[String] = {
    path.split("/")
  }

  private def getRoot:Directory = {

    if(rootAddress == null){
      createNewRoot
    }

    Directory(accessManager.get(rootAddress))
  }

  private def createNewRoot = {

    log info "Creating root directory"

    val directory = Directory.emptyDirectory(null)

    rootAddress = accessManager.add(directory.resource)

    configAccessor.update(_ + ((FSConfigAccessor.ROOT_ADDRESS, rootAddress)))
  }

  private def splitPath(path:String):(String, String) = {

    val components = getPathComponents(path)

    val name = components.lastOption match{
      case Some(x) => x
      case None => throw new FSException("Name is not specified")
    }

    val parentPath = path.replaceFirst(name + "$", "")

    (parentPath, path)
  }
}