package org.aphreet.c3.platform.remote.api.ws

import org.aphreet.c3.platform.remote.api.management._
import javax.jws.{WebMethod, WebService}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 24, 2010
 * Time: 12:31:34 AM
 * To change this template use File | Settings | File Templates.
 */

@WebService{val serviceName="ManagementService", val targetNamespace="remote.c3.aphreet.org"}
trait PlatformWSManagementEndpoint extends PlatformManagementService{

  @WebMethod
  def listStorages:Array[StorageDescription]

  @WebMethod
  def listStorageTypes:Array[String]

  @WebMethod
  def createStorage(stType:String, path:String)

  @WebMethod
  def removeStorage(id:String)

  @WebMethod
  def migrate(source:String, target:String)

  @WebMethod
  def setStorageMode(id:String, mode:String)

  @WebMethod
  def setPlatformProperty(key:String, value:String)

  @WebMethod
  def platformProperties:Array[Pair]

  @WebMethod
  def listTasks:Array[TaskDescription]

  @WebMethod
  def listFinishedTasks:Array[TaskDescription]

  @WebMethod
  def setTaskMode(taskId:String, mode:String)

  @WebMethod
  def listTypeMappigs:Array[TypeMapping]

  @WebMethod
  def addTypeMapping(mimeType:String, storage:String, versioned:java.lang.Short)

  @WebMethod
  def removeTypeMapping(mimeType:String)

  @WebMethod
  def listSizeMappings:Array[SizeMapping]

  @WebMethod
  def addSizeMapping(size:java.lang.Long, storage:String, versioned:java.lang.Integer)

  @WebMethod
  def removeSizeMapping(size:java.lang.Long)

  @WebMethod
  def listUsers:Array[Pair]

  @WebMethod
  def addUser(name:String, password:String, role:String)

  @WebMethod
  def updateUser(name:String, password:String, role:String)

  @WebMethod
  def deleteUser(name:String)

  @WebMethod
  def statistics:Array[Pair]

}