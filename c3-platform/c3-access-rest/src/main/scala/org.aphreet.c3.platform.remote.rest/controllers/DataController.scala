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
 * 3. Neither the name of the iFunSoftware nor the names of its contributors
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

package org.aphreet.c3.platform.remote.rest.controllers

import org.springframework.web.context.ServletContextAware
import javax.servlet.ServletContext
import org.aphreet.c3.platform.access.AccessManager
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.exception.ResourceNotFoundException
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import java.io.{FileOutputStream, File, BufferedOutputStream}
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.io.{IOUtils, FileCleaningTracker}
import org.aphreet.c3.platform.resource.{DataStream, ResourceVersion, Resource}
import java.util.UUID
import org.apache.commons.fileupload.servlet.{FileCleanerCleanup, ServletFileUpload}
import org.apache.commons.fileupload.FileItem
import org.aphreet.c3.platform.remote.rest.response.fs.{FSNodeData, FSNode, FSDirectory}
import org.aphreet.c3.platform.remote.rest.response.{DirectoryResult, ResourceResult}
import org.aphreet.c3.platform.filesystem.{FSManager, Directory, Node}
import org.aphreet.c3.platform.domain.Domain
import org.aphreet.c3.platform.accesscontrol._
import org.aphreet.c3.platform.remote.rest.WrongRequestException
import collection.mutable
import org.apache.commons.codec.binary.Base64
import org.aphreet.c3.platform.filesystem.NodeRef
import scala.Some
import org.aphreet.c3.platform.query.QueryManager
import org.aphreet.c3.platform.metadata.TransientMetadataManager

class DataController extends AbstractController with ServletContextAware with RestController{

  var servletContext: ServletContext = _

  @Autowired
  var queryManager: QueryManager = _

  @Autowired
  var accessControlManager: AccessControlManager = _

  @Autowired
  var accessManager: AccessManager = _

  @Autowired
  var transientMetadataManager: TransientMetadataManager = _

  @Autowired
  var filesystemManager: FSManager = _


  def setServletContext(context: ServletContext) {
    servletContext = context
  }


  protected def sendResourceData(resource: Resource, versionNumber: Int, accessTokens: AccessTokens, resp: HttpServletResponse) {

    accessTokens.checkAccess(resource)

    val version =
      if (versionNumber == -1) resource.versions.size
      else versionNumber

    if (version > 0 && resource.versions.size >= version) {

      val resourceVersion = resource.versions(version - 1)

      resp.reset()
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.setContentLength(resourceVersion.data.length.toInt)

      resp.setContentType(resource.contentType)

      val os = new BufferedOutputStream(resp.getOutputStream)

      try {
        resourceVersion.data.writeTo(os)
      } finally {
        os.close()
        resp.flushBuffer()
      }

    } else {
      throw new ResourceNotFoundException("Incorrect version number")
    }
  }

  protected def sendDirectoryContents(node: Node, childMeta:String, needsData:Boolean, contentType: String, accessTokens: AccessTokens, response: HttpServletResponse) {
    accessTokens.checkAccess(node.resource)

    val directory = node.asInstanceOf[Directory]

    val metaKeys:Set[String] = if(childMeta != null) childMeta.split(",").filter(!_.isEmpty).toSet else Set()

    val fsDirectory = if(!metaKeys.isEmpty || needsData){

      val children:List[Option[FSNode]] = directory.children.map((child:NodeRef) => {

        accessManager.getOption(child.address) match {
          case Some(resource) => {
            Some(new FSNode(child,
              resource.metadata.asMap.filterKeys(metaKeys.contains(_)),

              if(needsData){
                resource.versions.last.systemMetadata("c3.data.length") match {
                  case Some(value) => if(value.toLong < 10240) resource.versions.last else null
                  case None => null
                }
              } else
                null
              ))
          }
          case None => None
        }
      }).toList

      FSDirectory.fromNodeAndChildren(directory, children.flatten)
    }else{
      FSDirectory.fromNode(directory)
    }

    getResultWriter(contentType).writeResponse(
      new DirectoryResult(fsDirectory), response)
  }

  def getAccessTokens(action: Action, request: HttpServletRequest): AccessTokens = {

    val map = new mutable.HashMap[String, String]

    val headerEnum = request.getHeaderNames

    while (headerEnum.hasMoreElements) {
      val headerName = headerEnum.nextElement().toString

      val headerValue = request.getHeader(headerName)

      map.put(headerName, headerValue)
    }

    map.put("x-c3-request-uri", request.getRequestURI)

    accessControlManager.retrieveAccessTokens(RemoteAccess, action, map.toMap)
  }

  protected def sendResourceMetadata(address: String, contentType: String, accessTokens: AccessTokens, system: Boolean, resp: HttpServletResponse) {

    val resource = accessManager.get(address)

    sendMetadata(resource, contentType, accessTokens, resp)

  }

  protected def sendMetadata(resource: Resource, contentType: String, accessTokens: AccessTokens, resp: HttpServletResponse) {

    accessTokens.checkAccess(resource)

    resp.setStatus(HttpServletResponse.SC_OK)

    getResultWriter(contentType).writeResponse(new ResourceResult(resource), resp)
  }

  protected def addNonPersistentMetadata(resource: Resource, extMeta: String) {

    if (extMeta != null) {

      val keys = extMeta.split(",")

      transientMetadataManager.getTransientMetadata(resource.address, keys.toSet) foreach {
        case (k, v) => resource.transientMetadata(k) = v
      }
    }

  }

  protected def executeDataUpload(resource: Resource,
                                  accessTokens: AccessTokens,
                                  request: HttpServletRequest,
                                  response: HttpServletResponse,
                                  processStore: () => Unit) {

    if (isMultipartRequest(request)) {

      log debug "Starting data upload"

      val factory = createDiskFileItemFactory

      val upload = new ServletFileUpload(factory)

      log debug "Parsing request"

      val iterator = upload.parseRequest(request).iterator

      val metadata = new mutable.HashMap[String, String]

      var data: DataStream = null
      var tmpFile: File = null

      while (iterator.hasNext) {
        val item = iterator.next.asInstanceOf[FileItem]

        if (item.isFormField) {
          val value = item.getString("UTF-8") //correct string in UTF-16
          metadata.put(item.getFieldName, value)
        } else {

          if (data != null) {
            throw new WrongRequestException("Too many data fields")
          }

          if (item.isInMemory) {
            data = DataStream.create(item.get)
          } else {
            tmpFile = new File(factory.getRepository, UUID.randomUUID.toString)
            factory.getFileCleaningTracker.track(tmpFile, request)
            item.write(tmpFile)
            data = DataStream.create(tmpFile)
          }
        }
      }

      log debug "Parse complete"

      try {
        if (data != null) {
          val version = new ResourceVersion
          version.data = data
          resource.addVersion(version)
        }

        resource.metadata ++= metadata

        accessTokens.updateMetadata(resource)

        log debug "Executing callback"

        runResourceStore(resource, request, processStore)

        log debug "Upload done"

      } finally {
        if (tmpFile != null) tmpFile.delete
      }

    } else {

      val metadata = getMetadata(request)

      if(request.getContentLength > 0) {
        val factory = createDiskFileItemFactory

        val tmpFile = new File(factory.getRepository, UUID.randomUUID.toString)

        factory.getFileCleaningTracker.track(tmpFile, request)

        val os = new FileOutputStream(tmpFile)

        try{
          IOUtils.copy(request.getInputStream, os)
        }finally {
          os.close()
        }

        val version = new ResourceVersion
        version.data = DataStream.create(tmpFile)
        resource.addVersion(version)
      }

      getMetadataToDelete(request).foreach(resource.metadata.remove(_))

      resource.metadata ++= metadata

      accessTokens.updateMetadata(resource)
      log debug "Executing callback"
      runResourceStore(resource, request, processStore)
      log debug "Upload done"
    }
  }

  protected def runResourceStore(resource:Resource, request:HttpServletRequest, callback : () => Unit){
    if(request.getMethod == "POST"){
      if(resource.versions.isEmpty){
        throw new WrongRequestException("No data in create resource request")
      }
    }

    callback()
  }

  protected def createDiskFileItemFactory: DiskFileItemFactory = {
    val fileCleaningTacker: FileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(servletContext)

    val factory = new DiskFileItemFactory()
    factory.setFileCleaningTracker(fileCleaningTacker)

    factory
  }

  private def isMultipartRequest(request: HttpServletRequest): Boolean = {

    val contentType = request.getContentType
    if (contentType != null) {
      if (contentType.toLowerCase.startsWith("multipart/")) {
        return true
      }
    }

    false
  }

  protected def getCurrentDomainId(tokens: AccessTokens): String = {
    tokens.tokenForName(Domain.ACCESS_TOKEN_NAME) match {
      case Some(token) => token.id
      case None => throw new AccessControlException("Failed to locate current domain in the access tokens")
    }
  }

  protected def getMetadata(request: HttpServletRequest): Map[String, String] = {
    import scala.collection.JavaConversions._
    val metadataHeaders = request.getHeaders("x-c3-metadata")

    (for {
      header <- metadataHeaders
      keyValue = header.toString.split(":", 2)
      key = keyValue(0)
      value = new String(Base64.decodeBase64(keyValue(1).getBytes("UTF-8")), "UTF-8")
    } yield (key, value)).toMap
  }

  protected def getMetadataToDelete(request: HttpServletRequest): List[String] = {
    import scala.collection.JavaConversions._
    val metadataHeaders = request.getHeaders("x-c3-metadata-delete")

    (for {
      header <- metadataHeaders
    } yield header.toString).toList
  }
}