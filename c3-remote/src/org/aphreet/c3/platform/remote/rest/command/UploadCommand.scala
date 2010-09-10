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

package org.aphreet.c3.platform.remote.rest.command

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.apache.commons.fileupload.servlet.{FileCleanerCleanup, ServletFileUpload}
import org.apache.commons.io.FileCleaningTracker
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.aphreet.c3.platform.resource.{Resource, ResourceVersion, DataWrapper}
import java.util.UUID
import org.apache.commons.fileupload.FileItem
import java.io.File
import collection.mutable.HashMap
import javax.servlet.ServletContext
import org.aphreet.c3.platform.exception.ResourceNotFoundException
import org.aphreet.c3.platform.remote.rest.{ResourceRequest, URIParseException, Command}


abstract class UploadCommand(override val req: HttpServletRequest,
                             override val resp: HttpServletResponse,
                             val context: ServletContext) extends HttpCommand(req, resp) {
  val metadata = new HashMap[String, String]
  var data: DataWrapper = null
  var tmpFile: File = null
  val factory = newDiskFileItemFactory

  override def execute {

    try {
      if (requestType == ResourceRequest && ServletFileUpload.isMultipartContent(req)) {

        val upload = new ServletFileUpload(factory)

        val iterator = upload.parseRequest(req).iterator

        while (iterator.hasNext) {
          val item = iterator.next.asInstanceOf[FileItem]

          if (item.isFormField) processField(item)
          else processFile(item)

        }

        completeUpload

      } else badRequest
    } catch {
      case e: WrongRequestException => badRequest
      case e: URIParseException => badRequest
      case e: ResourceNotFoundException => notFound
    }
  }

  def processField(item: FileItem) = {

    val value = item.getString("UTF-8") //correct string in UTF-16
    metadata.put(item.getFieldName, value)

  }

  def processFile(item: FileItem) = {

    if (data != null) throw new WrongRequestException

    if (item.isInMemory) {
      data = DataWrapper.wrap(item.get)
    } else {
      tmpFile = new File(factory.getRepository, UUID.randomUUID.toString)
      item.write(tmpFile)
      data = DataWrapper.wrap(tmpFile)
    }
  }

  def completeUpload {
    try {
      val version = new ResourceVersion
      version.data = data


      val resource = getResource
      resource.metadata ++= metadata
      resource.addVersion(version)

      processUpload(resource)
    } catch {
      case e: ResourceNotFoundException =>
        notFound
    } finally {
      if (tmpFile != null) tmpFile.delete
    }
  }

  def getResource: Resource

  def processUpload(resource: Resource)

  def newDiskFileItemFactory: DiskFileItemFactory = {
    val fileCleaningTacker: FileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(context)

    val factory = new DiskFileItemFactory()
    factory.setFileCleaningTracker(fileCleaningTacker)

    factory
  }

}

class WrongRequestException extends Exception
