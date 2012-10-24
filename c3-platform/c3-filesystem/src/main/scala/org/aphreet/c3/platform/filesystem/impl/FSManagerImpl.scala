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

package org.aphreet.c3.platform.filesystem.impl

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.filesystem._
import org.apache.commons.logging.LogFactory
import annotation.tailrec
import org.aphreet.c3.platform.statistics.StatisticsManager
import org.aphreet.c3.platform.task.TaskManager
import java.lang.IllegalStateException
import org.aphreet.c3.platform.access.{ResourceOwner, AccessManager}
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.common.ComponentGuard

@Component("fsManager")
class FSManagerImpl extends FSManager with FSManagerInternal with ResourceOwner with ComponentGuard{

  val log = LogFactory getLog getClass

  @Autowired
  var accessManager:AccessManager = _

  @Autowired
  var configAccessor:FSConfigAccessor = _

  @Autowired
  var statisticsManager:StatisticsManager = _

  @Autowired
  var taskManager:TaskManager = _

  @Autowired
  var directoryUpdater:FSDirectoryUpdater = _

  var fsRoots:Map[String, String] = Map()

  @PostConstruct
  def init(){

    log info "Starting Filesystem manager"

    accessManager.registerOwner(this)

    fsRoots = configAccessor.load
  }

  @PreDestroy
  def destroy(){

    letItFall{
      accessManager.unregisterOwner(this)
    }
  }

  def getNode(domainId:String, path:String):Node = {
    getFSNode(domainId, path)
  }

  def deleteNode(domainId:String, path:String) {

    val node = getNode(domainId, path)

    accessManager.delete(node.resource.address)
  }

  def moveNode(domainId:String, oldPath:String, newPath:String){

    val oldPathAndName = splitPath(oldPath)

    val newPathAndName = splitPath(newPath)


    val currentParent = getFSNode(domainId, oldPathAndName._1)

    val newParent = getFSNode(domainId, newPathAndName._1)

    if(!currentParent.isDirectory) throw new FSException("Current parent is not a directory")

    if(!newParent.isDirectory) throw new FSException("Current parent is not a directory")

    val nodeRef = currentParent.asInstanceOf[Directory].getChild(oldPathAndName._2) match {
      case Some(n) => n
      case None => throw new FSException("Can't find specified node")
    }

    val nodeAddress = nodeRef.address

    val node = Node.fromResource(accessManager.get(nodeAddress))

    if(currentParent.resource.address == newParent.resource.address){

        node.resource.systemMetadata.put(Node.NODE_FIELD_NAME, newPathAndName._2)
        accessManager.update(node.resource)

        directoryUpdater ! ScheduleMsg(currentParent.resource.address,
          FSDirectoryTask(UPDATE, NodeRef(newPathAndName._2, nodeRef.address, nodeRef.leaf)))

    }else{

      val oldDir = Directory(accessManager.get(currentParent.resource.address))
      val newDir = Directory(accessManager.get(newParent.resource.address))

      node.resource.systemMetadata.put(Node.NODE_FIELD_NAME, newPathAndName._2)
      node.resource.systemMetadata.put(Node.NODE_FIELD_PARENT, newDir.resource.address)

      accessManager.update(node.resource)

      directoryUpdater ! ScheduleMsg(oldDir.resource.address,
        FSDirectoryTask(DELETE, NodeRef(oldPathAndName._2, nodeAddress, nodeRef.leaf)))
      directoryUpdater ! ScheduleMsg(newParent.resource.address,
        FSDirectoryTask(ADD, NodeRef(newPathAndName._2, nodeAddress, nodeRef.leaf)))
    }
  }

  override def resourceCanBeDeleted(resource:Resource):Boolean = {
    resource.systemMetadata.get(Node.NODE_FIELD_TYPE) match{
      case None => true
      case Some(nodeType) => {

        var canDelete = false

        if(nodeType == Node.NODE_TYPE_DIR){
          val directory = Directory(resource)
          if(directory.getChildren.isEmpty){
            canDelete = fsRoots.values.forall(_ != resource.address)
          }else{
            canDelete = false
          }
        }else{
          canDelete = true
        }

        canDelete
      }
    }
  }

  override def resourceCanBeUpdated(resource:Resource):Boolean = {
    resource.systemMetadata.get(Node.NODE_FIELD_TYPE) match{
      case None => true
      case Some(nodeType) =>
        if(nodeType == Node.NODE_TYPE_DIR){
          try{
            //trying to create a directory from provided ByteStream
            Directory(resource)
            true
          }catch{
            case e: Throwable => false
          }
        }else{
          true
        }
    }
  }

  override def deleteResource(resource:Resource) {

    resource.systemMetadata.get(Node.NODE_FIELD_NAME) match {
      case None => //it seems that resource is not a part of FS, skipping
      case Some(name) =>
        resource.systemMetadata.get(Node.NODE_FIELD_PARENT) match{
          case Some(parentAddress) => {
              directoryUpdater ! ScheduleMsg(parentAddress, FSDirectoryTask(DELETE, NodeRef(name, resource.address, false)))
          }
          case None =>
        }
    }
  }

  def createFile(domainId:String, fullPath:String, resource:Resource) {

    val pathAndName = splitPath(fullPath)

    val path = pathAndName._1
    val name = pathAndName._2

    if(log.isDebugEnabled){
      log.debug("Creating file " + name + " at path " + path)
    }

    addNodeToDirectory(domainId, path, name, File.createFile(resource, domainId, name))

  }

  def createDirectory(domainId:String, fullPath:String) {

    val pathAndName = splitPath(fullPath)

    val path = pathAndName._1
    val name = pathAndName._2

    if(log.isDebugEnabled){
      log.debug("Creating directory " + name + " at path " + path)
    }

    addNodeToDirectory(domainId, path, name, Directory.emptyDirectory(domainId, name))
  }

  def lookupResourcePath(address:String):Option[String] = {
    try{

      val pathComponents = lookupResourcePath(address, List[String]())

      if(pathComponents.isEmpty){
        None
      }else{
        Some(pathComponents.foldLeft("")(_ + "/" + _))
      }
    }catch{
      case e: Throwable => None
    }
  }

  @tailrec
  private def lookupResourcePath(address:String, pathComponents:List[String]):List[String] = {

    val resource = accessManager.get(address)

    val name = resource.systemMetadata.get(Node.NODE_FIELD_NAME)

    resource.systemMetadata.get(Node.NODE_FIELD_PARENT) match{
      case Some(value) => lookupResourcePath(value, name.get :: pathComponents)
      case None => pathComponents
    }

  }

  def fileSystemRoots:Map[String, String] = fsRoots

  def importFileSystemRoot(domainId:String, address:String) {

    fsRoots.get(domainId) match{
      case Some(x) =>
        if(x != address)
          throw new FSException("Can't import FS root - root already exists")
      case None => {
        this.synchronized{

          log info "Adding new root: " + domainId + " => " + address

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
      case Some(value) => throw new FSWrongRequestException("Node with name: " + name +  " already exists")
      case None =>
    }

    newNode.resource.systemMetadata.put(Node.NODE_FIELD_PARENT, directory.resource.address)
    val newAddress = accessManager.add(newNode.resource)

    directoryUpdater ! ScheduleMsg(directory.resource.address, FSDirectoryTask(ADD, NodeRef(name, newAddress, !newNode.isDirectory)))
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

  def splitPath(path:String):(String, String) = {

    if(log.isDebugEnabled){
      log debug "Splitting path: " + path
    }

    val components = getPathComponents(path)

    val name = components.lastOption match{
      case Some(x) => x
      case None => throw new FSException("Name is not specified")
    }

    val parentPath = path.substring(0, path.length - name.length - 1)

    (parentPath, name)

  }

  def startFilesystemCheck(){

    if(taskManager.taskList.filter(_.name == classOf[FSCheckTask].getSimpleName).isEmpty){
      val task = new FSCheckTask(accessManager, statisticsManager, fsRoots)
      taskManager.submitTask(task)
    }else{
      throw new IllegalStateException("Task already started")
    }
  }

  def executeDirectoryTasks(address:String, tasks:List[FSDirectoryTask]){
    val directory = Node.fromResource(accessManager.get(address)).asInstanceOf[Directory]

    for (task <- tasks){
      task.action match {
        case ADD => directory.addChild(task.nodeRef)
        case DELETE => directory.removeChild(task.nodeRef.name)
        case UPDATE => {
          directory.getChildren.filter(_.address == task.nodeRef.address).headOption match {
            case Some(oldNodeRef) => {
              val oldName = oldNodeRef.name
              directory.removeChild(oldName)
              directory.addChild(task.nodeRef)
            }
            case None => {
              log.warn("Request to rename missing resource " + task.nodeRef.address + " in directory " + address)
            }
          }
        }
        case _ =>
      }
    }

    accessManager.update(directory.resource)
  }
}