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

  var fsRoots:Map[String, String] = Map()

  @Autowired
  def setAccessManager(manager:AccessManager) = {accessManager = manager}

  @Autowired
  def setConfigAccessor(accessor:FSConfigAccessor) = {configAccessor = accessor}

  @PostConstruct
  def init{

    log info "Starting Filesystem manager"

    fsRoots = configAccessor.load
  }

  def getNode(domainId:String, path:String):Node = {
    getFSNode(domainId, path)
  }

  def deleteNode(domainId:String, path:String) = {

    val components = getPathComponents(path)

    val nodeToDelete = components.lastOption match{
      case Some(x) => x
      case None => throw new FSException("Can't remove root node")
    }

    val parentPath = path.replaceFirst(nodeToDelete + "$", "")


    val node = getNode(domainId, path)

    if(node.isDirectory){
      val directory = node.asInstanceOf[Directory]
      if(!directory.getChildren.isEmpty){
        throw new FSException("Can't remove non-empty directory")
      }
    }

    val parentNode = getNode(domainId, parentPath).asInstanceOf[Directory]

    parentNode.removeChild(nodeToDelete)

    accessManager.update(parentNode.resource)
    accessManager.delete(node.resource.address)
  }

  def createFile(domainId:String, fullPath:String, resource:Resource) = {

    val pathAndName = splitPath(fullPath)

    val path = pathAndName._1
    val name = pathAndName._2

    if(log.isDebugEnabled){
      log.debug("Creating file " + name + " at path " + path)
    }

    addNodeToDirectory(domainId, path, name, File.createFile(resource, domainId, name))

  }

  def createDirectory(domainId:String, fullPath:String) = {

    val pathAndName = splitPath(fullPath)

    val path = pathAndName._1
    val name = pathAndName._2

    if(log.isDebugEnabled){
      log.debug("Creating directory " + name + " at path " + path)
    }

    addNodeToDirectory(domainId, path, name, Directory.emptyDirectory(domainId, name))
  }

  def fileSystemRoots:Map[String, String] = fsRoots

  def importFileSystemRoot(domainId:String, address:String) = {

    fsRoots.get(domainId) match{
      case Some(x) => throw new FSException("Can't import FS root - root already exists")
      case None => {
        this.synchronized{
          val map:Map[String, String] = fsRoots + ((domainId, address))

          configAccessor.store(map)

          fsRoots = configAccessor.load
        }
      }
    }
  }

  private def addNodeToDirectory(domainId:String, path:String, name:String, newNode:Node){

    val node = getFSNode(domainId, path)

    var directory:Directory = null

    if(node.isDirectory){
      directory = node.asInstanceOf[Directory]
    }else{
      throw new FSException("Can't add node to file")
    }

    directory.getChild(name) match{
      case Some(value) => throw new FSWrongRequestException("Node with name: " + name +  "already exists")
      case None =>
    }


    try{
      accessManager.lock(directory.resource.address)

      //refreshing directory instance
      directory = Node.fromResource(accessManager.get(directory.resource.address)).asInstanceOf[Directory]

      newNode.resource.systemMetadata.put(Node.NODE_FIELD_PARENT, directory.resource.address)

      val newAddress = accessManager.add(newNode.resource)

      directory.addChild(NodeRef(name, newAddress, !newNode.isDirectory))

      accessManager.update(directory.resource)

    }catch{
      case e:StorageException =>
    }finally {
      accessManager.unlock(directory.resource.address)
    }
  }

  private def getFSNode(domainId:String, path:String):Node = {

    if(log.isDebugEnabled){
      log.debug("Looking for node for path: " + path)
    }

    val pathComponents = getPathComponents(path).filter(_.length > 0)

    var resultNode:Node = getRoot(domainId)

    for(directoryName <- pathComponents){

      if(log.isTraceEnabled){
        log.trace("Getting node: " + directoryName)
      }

      if(!resultNode.isDirectory){
        throw new FSException("Found file, expected directory")
      }

      val nodeRef = resultNode.asInstanceOf[Directory].getChild(directoryName) match {
        case Some(a) => a
        case None => throw new FSNotFoundException("Specified path does not exists")
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

  private def getRoot(domainId:String):Directory = {

    val rootAddress = fsRoots.get(domainId) match{
      case Some(x) =>x
      case None => createNewRoot(domainId)
    }

    Directory(accessManager.get(rootAddress))
  }

  private def createNewRoot(domainId:String) = {

    log info "Creating root directory for domain: " + domainId

    val directory = Directory.emptyDirectory(domainId, null)

    val rootAddress = accessManager.add(directory.resource)

    this.synchronized{
      val map:Map[String, String] = fsRoots + ((domainId, rootAddress))

      configAccessor.store(map)

      fsRoots = configAccessor.load
    }

    rootAddress
  }

  private def splitPath(path:String):(String, String) = {

    if(log.isDebugEnabled){
      log debug "Splitting path: " + path
    }

    val components = getPathComponents(path)

    val name = components.lastOption match{
      case Some(x) => x
      case None => throw new FSException("Name is not specified")
    }

    val parentPath = path.replaceFirst(name + "$", "")

    (parentPath, name)
  }
}