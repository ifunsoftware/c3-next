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

package org.aphreet.c3.platform.remote.rest.controllers

import org.springframework.web.context.ServletContextAware
import javax.servlet.ServletContext
import org.aphreet.c3.platform.access.AccessManager
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.exception.ResourceNotFoundException
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import collection.mutable.HashMap
import java.io.{File, BufferedOutputStream}
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.io.FileCleaningTracker
import org.aphreet.c3.platform.resource.{DataStream, ResourceVersion, Resource}
import java.util.UUID
import org.apache.commons.fileupload.servlet.{FileCleanerCleanup, ServletFileUpload}
import org.apache.commons.fileupload.FileItem
import org.aphreet.c3.platform.remote.rest.response.fs.{FSNode, FSDirectory}
import org.aphreet.c3.platform.remote.rest.response.{DirectoryResult, ResourceResult}
import org.aphreet.c3.platform.filesystem.{NodeRef, FSManager, Directory, Node}
import org.aphreet.c3.platform.domain.Domain
import org.aphreet.c3.platform.accesscontrol.{AccessControlException, AccessTokens, Action, AccessControlManager}
import org.aphreet.c3.platform.remote.rest.WrongRequestException

class DataController extends AbstractController with ServletContextAware {

  var servletContext: ServletContext = _

  @Autowired
  var accessControlManager: AccessControlManager = _

  @Autowired
  var accessManager: AccessManager = _

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

      resource.metadata.get(Resource.MD_CONTENT_TYPE) match {
        case Some(x) => resp.setContentType(x)
        case None =>
      }

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

  protected def sendDirectoryContents(node: Node, childMeta:String, contentType: String, accessTokens: AccessTokens, response: HttpServletResponse) {
    accessTokens.checkAccess(node.resource)

    val directory = node.asInstanceOf[Directory]

    val fsDirectory = if(childMeta != null){
      val metaKeys = childMeta.split(",").filter(!_.isEmpty)

      val children = directory.getChildren.map((child:NodeRef) => new FSNode(child, {

        accessManager.getOption(child.address) match {
          case Some(resource) => resource.metadata.filterKeys(metaKeys.contains(_))
          case None => Map()
        }
      }))

      FSDirectory.fromNodeAndChildren(directory, children)
    }else{
      FSDirectory.fromNode(directory)
    }

    getResultWriter(contentType).writeResponse(
      new DirectoryResult(fsDirectory), response)
  }

  protected def getAccessTokens(action: Action, request: HttpServletRequest): AccessTokens = {

    val map = new HashMap[String, String]

    val headerEnum = request.getHeaderNames

    while (headerEnum.hasMoreElements) {
      val headerName = headerEnum.nextElement().toString

      val headerValue = request.getHeader(headerName)

      map.put(headerName, headerValue)
    }

    map.put("x-c3-request-uri", request.getRequestURI)

    accessControlManager.retrieveAccessTokens(action, map.toMap)
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

  protected def addNonPersistentMetadata(resource: Resource, extMeta: String) = {

    if (extMeta != null) {

      val keys = extMeta.split(",")

      //Replace this with something like strategy if future if we need more fields
      for (key <- keys) {
        log info "Processing extended meta: " + key
        if (key == "c3.ext.fs.path") {
          val value = filesystemManager.lookupResourcePath(resource.address)
          resource.systemMetadata.put(key, value)
        }
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

      val metadata = new HashMap[String, String]

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

        processStore()

        log debug "Upload done"

      } finally {
        if (tmpFile != null) tmpFile.delete
      }

    } else {
      throw new WrongRequestException("Multipart request expected")
    }
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
}