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

/**
 * Created by IntelliJ IDEA.
 * User: malygm
 * Date: May 5, 2010
 * Time: 4:57:43 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class UploadCommand(override val req:HttpServletRequest,
                  override val resp:HttpServletResponse,
                  val context:ServletContext) extends HttpCommand(req, resp){

  val metadata = new HashMap[String, String]
  var data:DataWrapper = null
  var tmpFile:File = null
  val factory = newDiskFileItemFactory

  override def execute{

    try{
      if(requestType == ResourceRequest && ServletFileUpload.isMultipartContent(req)){

        val upload = new ServletFileUpload(factory)

        val iterator = upload.parseRequest(req).iterator

        while(iterator.hasNext){
          val item = iterator.next.asInstanceOf[FileItem]

          if(item.isFormField) processField(item)
          else processFile(item)

        }

        completeUpload

      }else badRequest
    }catch{
      case e:WrongRequestException => badRequest
      case e:URIParseException => badRequest
      case e:ResourceNotFoundException => notFound
    }
  }

  def processField(item:FileItem) = {
    val value = item.getString("UTF-8")//correct string in UTF-16
    metadata.put(item.getFieldName, value)


/*    System.err.println("Upload:")
    System.err.println(value.getBytes.toString)
    System.err.println(value)

    val bytes:Array[Byte] = value.getBytes("UTF-8")
    System.err.write(bytes)
    System.err.println
*/
  }

  def processFile(item:FileItem) = {

    if(data != null) throw new WrongRequestException

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


      val resource = getResource
      resource.metadata ++ metadata
      resource.addVersion(version)

     processUpload(resource)

    }finally{
      if(tmpFile != null) tmpFile.delete
    }
  }

  def getResource:Resource

  def processUpload(resource:Resource)

  def newDiskFileItemFactory:DiskFileItemFactory = {
    val fileCleaningTacker:FileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(context)

    val factory = new DiskFileItemFactory()
    factory.setFileCleaningTracker(fileCleaningTacker)

    factory
  }

}

class WrongRequestException extends Exception
