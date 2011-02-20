/**
 * Copyright (c) 2010, Mikhail Malygin
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

import org.aphreet.c3.platform.access.AccessManager
import org.aphreet.c3.platform.auth.exception.AuthFailedException
import org.aphreet.c3.platform.resource._
import org.aphreet.c3.platform.remote.rest.response._

import org.apache.commons.io.FileCleaningTracker
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.servlet.{ServletFileUpload, FileCleanerCleanup}
import org.apache.commons.fileupload.FileItem

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.beans.factory.annotation.Autowired

import java.util.UUID
import java.io.{File, BufferedOutputStream}

import javax.servlet.http._
import javax.servlet.ServletContext


import collection.mutable.HashMap
import org.springframework.web.context.ServletContextAware

@Controller
@RequestMapping(Array("/resource"))
class ResourceController extends AbstractController with ServletContextAware{

  var accessManager:AccessManager = _

  var servletContext:ServletContext = _

  @Autowired
  def setAccessManager(manager:AccessManager) = {
    accessManager = manager
  }

  def setServletContext(context:ServletContext) = {
    servletContext = context
  }

  @RequestMapping(value =  Array("/{address}"),
                  method = Array(RequestMethod.GET))
  def getResource(@PathVariable address:String,
                  @RequestHeader(value = "x-c3-auth", required = false) authHeader:String,
                  @RequestHeader(value = "x-c3-type", required = false) contentType:String,
                  request:HttpServletRequest,
                  response:HttpServletResponse) =
  {
    val currentUser = getCurrentUser(authHeader, request.getRequestURI)

    val showSystem = request.getParameter("system") != null

    sendResourceMetadata(address, contentType, currentUser, showSystem, response)
  }


  @RequestMapping(value =  Array("/{address}/metadata"),
                  method = Array(RequestMethod.GET))
  def getResourceMetadata(@PathVariable address:String,
                  @RequestHeader(value = "x-c3-auth", required = false) authHeader:String,
                  @RequestHeader(value = "x-c3-type", required = false) contentType:String,
                  request:HttpServletRequest,
                  response:HttpServletResponse) =
  {

    val currentUser = getCurrentUser(authHeader, request.getRequestURI)

    val showSystem = request.getParameter("system") != null

    sendResourceMetadata(address, contentType, currentUser, showSystem, response)
  }

  @RequestMapping(value =  Array("/{address}/data"),
                  method = Array(RequestMethod.GET))
  def getResourceData(@PathVariable address:String,
                  @RequestHeader(value = "x-c3-auth", required = false) authHeader:String,
                  request:HttpServletRequest,
                  response:HttpServletResponse) =
  {

    val currentUser = getCurrentUser(authHeader, request.getRequestURI)

    val resource = accessManager.get(address)

    sendResourceData(resource, -1, currentUser, response)
  }

  @RequestMapping(value =  Array("/{address}/data/{version}"),
                  method = Array(RequestMethod.GET))
  def getResourceDataVersion(@PathVariable("address") address:String,
                  @PathVariable("version") version:Int,
                  @RequestHeader(value = "x-c3-auth", required = false) authHeader:String,
                  request:HttpServletRequest,
                  response:HttpServletResponse) =
  {

    val currentUser = getCurrentUser(authHeader, request.getRequestURI)

    val resource = accessManager.get(address)

    sendResourceData(resource, version, currentUser, response)
  }

  def sendResourceMetadata(address:String, contentType:String, username:String, system:Boolean, resp:HttpServletResponse) = {

    val resource = accessManager.get(address)

    resp.setStatus(HttpServletResponse.SC_OK)

    writerSelector.selectWriterForType(contentType).writeResponse(new ResourceResult(resource), resp)

  }

  

  @RequestMapping(method = Array(RequestMethod.POST))
  def saveResource(@RequestHeader(value = "x-c3-auth", required = false) authHeader:String,
                   @RequestHeader(value = "x-c3-type", required = false) contentType:String,
                   request:HttpServletRequest,
                   response:HttpServletResponse) =
  {

    val currentUser = getCurrentUser(authHeader, request.getRequestURI)

    val resource = new Resource

    executeDataUpload(resource, currentUser, request, response, () => {
      resource.systemMetadata.put(Resource.MD_USER, currentUser)
      val ra = accessManager.add(resource)
      response.setStatus(HttpServletResponse.SC_CREATED)
      writerSelector.selectWriterForType(contentType).writeResponse(new UploadResult(new ResourceAddress(ra, 1)), response)
    })

  }

  @RequestMapping(value=Array("/{address}"), method = Array(RequestMethod.PUT))
  def updateResource(@PathVariable address:String,
                     @RequestHeader(value = "x-c3-auth", required = false) authHeader:String,
                     @RequestHeader(value = "x-c3-type", required = false) contentType:String,
                     request:HttpServletRequest,
                     response:HttpServletResponse) =

  {
    val currentUser = getCurrentUser(authHeader, request.getRequestURI)

    val resource = accessManager.get(address)

    resource.systemMetadata.get(Resource.MD_USER) match{
      case Some(u) =>
        if(u != currentUser)
          throw new AuthFailedException("Current user can't update specified resource")
      case None => 
    }

    executeDataUpload(resource, currentUser, request, response, () => {
      val ra = accessManager.update(resource)
      writerSelector.selectWriterForType(contentType).writeResponse(new UploadResult(new ResourceAddress(ra, resource.versions.length)), response)
    })

  }

  def executeDataUpload(resource:Resource,
                        currentUser:String,
                        request:HttpServletRequest,
                        response:HttpServletResponse,
                        processUpload:Function0[Unit]) =
  {

    if(ServletFileUpload.isMultipartContent(request)){

      val factory = createDiskFileItemFactory

      val upload = new ServletFileUpload(factory)

      val iterator = upload.parseRequest(request).iterator

      val metadata = new HashMap[String, String]

      var data:DataWrapper = null
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
            data = DataWrapper.wrap(item.get)
          } else {
            tmpFile = new File(factory.getRepository, UUID.randomUUID.toString)
            item.write(tmpFile)
            data = DataWrapper.wrap(tmpFile)
          }
        }

      }

      try {
        val version = new ResourceVersion
        version.data = data

        resource.metadata ++= metadata
        resource.addVersion(version)

        processUpload()

      } finally {
        if (tmpFile != null) tmpFile.delete
      }

    }else{
      throw new WrongRequestException("Multipart request expected")
    }
  }


  @RequestMapping(value=Array("/{address}"), method = Array(RequestMethod.DELETE))
  def deleteResource(@PathVariable address:String,
                     @RequestHeader(value = "x-c3-auth", required = false) authHeader:String,
                     request:HttpServletRequest,
                     response:HttpServletResponse) = {

    val currentUser = getCurrentUser(authHeader, request.getRequestURI)

    val resource = accessManager.get(address)

    resource.systemMetadata.get(Resource.MD_USER) match {
      case Some(u) => if(u != currentUser) throw new AuthFailedException("Current user can't delete this resource")
      case None =>
    }

    accessManager.delete(address)
  }

  def createDiskFileItemFactory: DiskFileItemFactory = {
    val fileCleaningTacker: FileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(servletContext)

    val factory = new DiskFileItemFactory()
    factory.setFileCleaningTracker(fileCleaningTacker)

    factory
  }

}