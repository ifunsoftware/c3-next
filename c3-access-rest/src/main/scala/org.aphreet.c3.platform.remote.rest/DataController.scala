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

package org.aphreet.c3.platform.remote.rest

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
import response.fs.FSDirectory
import response.{DirectoryResult, ResourceResult}
import org.aphreet.c3.platform.filesystem.{FSManager, Directory, Node}
import org.aphreet.c3.platform.domain._

class DataController extends AbstractController with ServletContextAware{

  var servletContext:ServletContext = _

  var domainManager:DomainManager = _

  var accessManager:AccessManager = _

  var filesystemManager:FSManager = _

  @Autowired
  def setAccessManager(manager:AccessManager) {
    accessManager = manager
  }

  @Autowired
  def setDomainManager(manager:DomainManager) {
    domainManager = manager
  }

  def setServletContext(context:ServletContext) {
    servletContext = context
  }

  @Autowired
  def setFilesystemManager(manager:FSManager) {filesystemManager = manager}

  protected def sendResourceData(resource:Resource, versionNumber:Int, domain:String, resp:HttpServletResponse) {

    checkDomainAccess(resource, domain)

    val version =
      if(versionNumber == -1) resource.versions.size
      else versionNumber

    if(version > 0 && resource.versions.size >= version){

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

    }else{
      throw new ResourceNotFoundException("Incorrect version number")
    }
  }

  protected def sendDirectoryContents(node:Node, contentType:String, domain:String, response:HttpServletResponse) {
    checkDomainAccess(node.resource, domain)
    getResultWriter(contentType).writeResponse(new DirectoryResult(FSDirectory.fromNode(node.asInstanceOf[Directory])), response)
  }

  protected def checkDomainAccess(resource:Resource, domainId:String) {
    resource.systemMetadata.get(Domain.MD_FIELD) match{
      case Some(id) => if(id != domainId) throw new DomainException("Requested resource does not belong to specified domain")
      case None =>
    }
  }

  protected def getRequestDomain(request:HttpServletRequest, readonly:Boolean):String = {

    var domain:Domain = null

    val requestDomain = request.getHeader("x-c3-domain")

    if(requestDomain != null){

      val requestUri = request.getRequestURI
      val date = request.getHeader("x-c3-date")

      val hashBase = requestUri + date + requestDomain

      val hash = request.getHeader("x-c3-sign")

      if(hash == null){
        throw new DomainException("Signature is empty")
      }

      domain = domainManager.checkDomainAccess(requestDomain, hash, hashBase)

    }else{
      domain = domainManager.getAnonymousDomain
    }

    domain.mode match{
      case DisabledMode => throw new DomainException("Domain is disabled")
      case FullMode => domain.id
      case ReadOnlyMode =>
        if(readonly){
          domain.id
        }else{
          throw new DomainException("Domain is readonly")
        }
    }

  }

  protected def sendResourceMetadata(address:String, contentType:String, domain:String, system:Boolean, resp:HttpServletResponse) {

    val resource = accessManager.get(address)

    sendMetadata(resource, contentType, domain, resp)

  }

  protected def sendMetadata(resource:Resource, contentType:String, domain:String, resp:HttpServletResponse){

    checkDomainAccess(resource, domain)

    resp.setStatus(HttpServletResponse.SC_OK)

    getResultWriter(contentType).writeResponse(new ResourceResult(resource), resp)
  }

  protected def addNonPersistentMetadata(resource:Resource, extMeta:String) = {

    if(extMeta != null){

      val keys = extMeta.split(",")

      //Replace this with something like strategy if future if we need more fields
      for(key <- keys){
        log info "Processing extended meta: " + key
        if(key == "c3.ext.fs.path"){
          val value = filesystemManager.lookupResourcePath(resource.address)
          resource.systemMetadata.put(key, value)
        }
      }
    }

  }

  protected def executeDataUpload(resource:Resource,
                                  domain:String,
                                  request:HttpServletRequest,
                                  response:HttpServletResponse,
                                  processUpload:() => Unit) {

    if(isMultipartRequest(request)){

      log debug "Starting data upload"

      val factory = createDiskFileItemFactory

      val upload = new ServletFileUpload(factory)

      log debug "Parsing request"

      val iterator = upload.parseRequest(request).iterator

      val metadata = new HashMap[String, String]

      var data:DataStream = null
      var tmpFile:File = null

      while (iterator.hasNext) {
        val item = iterator.next.asInstanceOf[FileItem]

        if (item.isFormField){
          val value = item.getString("UTF-8") //correct string in UTF-16
          metadata.put(item.getFieldName, value)
        }else{

          if(data != null){
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
        if(data != null){
          val version = new ResourceVersion
          version.data = data
          resource.addVersion(version)
        }

        resource.metadata ++= metadata

        log debug "Executing callback"

        processUpload()

        log debug "Upload done"

      } finally {
        if (tmpFile != null) tmpFile.delete
      }

    }else{
      throw new WrongRequestException("Multipart request expected")
    }
  }

  protected def createDiskFileItemFactory: DiskFileItemFactory = {
    val fileCleaningTacker: FileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(servletContext)

    val factory = new DiskFileItemFactory()
    factory.setFileCleaningTracker(fileCleaningTacker)

    factory
  }

  private def isMultipartRequest(request:HttpServletRequest):Boolean = {

    val contentType = request.getContentType
    if(contentType != null){
      if(contentType.toLowerCase.startsWith("multipart/")){
        return true
      }
    }

    false
  }
}