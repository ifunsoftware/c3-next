package org.aphreet.c3.platform.remote.rest.command

import org.aphreet.c3.platform.remote.rest.Command
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import java.util.UUID
import java.io.{File}
import org.aphreet.c3.platform.resource.{DataWrapper, ResourceVersion, Resource}
import javax.servlet.ServletContext
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.io.FileCleaningTracker
import org.apache.commons.fileupload.servlet.{FileCleanerCleanup, ServletFileUpload}
import org.apache.commons.fileupload.FileItem
import collection.mutable.HashMap

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 6, 2010
 * Time: 8:14:48 PM
 * To change this template use File | Settings | File Templates.
 */

class PostCommand(override val req:HttpServletRequest,
                  override val resp:HttpServletResponse,
                  val context:ServletContext) extends Command(req, resp){

  val metadata = new HashMap[String, String]
  var data:DataWrapper = null
  var tmpFile:File = null
  val factory = newDiskFileItemFactory

  override def execute{
    if(ServletFileUpload.isMultipartContent(req)){
      
      val upload = new ServletFileUpload(factory)
      
      val iterator = upload.parseRequest(req).iterator

      while(iterator.hasNext){
        val item = iterator.next.asInstanceOf[FileItem]
        
        if(item.isFormField) processField(item)
        else processFile(item)
      }

      completeUpload

    }else badRequest

  }

  def processField(item:FileItem) = {
    metadata.put(item.getFieldName, item.getString)
  }

  def processFile(item:FileItem) = {
    if(item.isInMemory){
      data = DataWrapper.wrap(item.get)
    }else{
      tmpFile = new File(factory.getRepository, UUID.randomUUID.toString)
      item.write(tmpFile)
      data = DataWrapper.wrap(tmpFile)
    }
  }

  def completeUpload{
    try{
      val version = new ResourceVersion
      version.data = data


      val resource = new Resource
      resource.metadata ++ metadata
      resource.addVersion(version)

      val ra = accessEndpoint.add(resource)
      resp.getWriter.println(ra)
      
    }finally{
      if(tmpFile != null){
        tmpFile.delete
      }
    }

  }

  def newDiskFileItemFactory:DiskFileItemFactory = {
    val fileCleaningTacker:FileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(context)

    val factory = new DiskFileItemFactory()
    factory.setFileCleaningTracker(fileCleaningTacker)

    factory
  }

}