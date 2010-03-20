package org.aphreet.c3.platform.remote.api.ws.impl

import org.aphreet.c3.platform.remote.api.management._
import javax.jws.{WebMethod, WebService}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.remote.api.ws.PlatformWSManagementEndpoint

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 1:31:55 PM
 * To change this template use File | Settings | File Templates.
 */

@Component
@WebService{val serviceName="ManagementService", val targetNamespace="remote.c3.aphreet.org"}
class PlatformWSManagementEndpointImpl extends PlatformWSManagementEndpoint{


  private var managementAdapter:PlatformManagementAdapter = null

  @Autowired
  private def setManagementAdapter(adapter:PlatformManagementAdapter) = {managementAdapter = adapter; println("setter invoked")}

  @WebMethod
  def listStorages:Array[StorageDescription] = managementAdapter.listStorages

  @WebMethod
  def listStorageTypes:Array[String] = managementAdapter.listStorageTypes

  @WebMethod
  def createStorage(stType:String, path:String) = managementAdapter.createStorage(stType, path)

  @WebMethod
  def removeStorage(id:String) = managementAdapter.removeStorage(id)

  @WebMethod
  def migrate(source:String, target:String) = managementAdapter.migrate(source, target)

  @WebMethod
  def setStorageMode(id:String, mode:String) = managementAdapter.setStorageMode(id, mode)

  @WebMethod
  def setPlatformProperty(key:String, value:String) = managementAdapter.setPlatformProperty(key, value)

  @WebMethod
  def platformProperties:Array[Pair] = managementAdapter.platformProperties

  @WebMethod
  def listTasks:Array[TaskDescription] = managementAdapter.listTasks

  @WebMethod
  def listFinishedTasks:Array[TaskDescription] = managementAdapter.listFinishedTasks

  @WebMethod
  def setTaskMode(taskId:String, mode:String) = managementAdapter.setTaskMode(taskId, mode)

  @WebMethod
  def listTypeMappigs:Array[TypeMapping] = managementAdapter.listTypeMappigs

  @WebMethod
  def addTypeMapping(mimeType:String, storage:String, versioned:java.lang.Short)
    = managementAdapter.addTypeMapping(mimeType, storage, versioned)

  @WebMethod
  def removeTypeMapping(mimeType:String) = managementAdapter.removeTypeMapping(mimeType)

  @WebMethod
  def listSizeMappings:Array[SizeMapping] = managementAdapter.listSizeMappings

  @WebMethod
  def addSizeMapping(size:java.lang.Long, storage:String, versioned:java.lang.Integer)
    = managementAdapter.addSizeMapping(size, storage, versioned)

  @WebMethod
  def removeSizeMapping(size:java.lang.Long) = managementAdapter.removeSizeMapping(size)

  @WebMethod
  def buildResourceList(targetDir:String) = managementAdapter.buildResourceList(targetDir)

  @WebMethod{val exclude=true}
  override def $tag:Int = super.$tag

}