package org.aphreet.c3.platform.remote.api.management

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 1:41:55 PM
 * To change this template use File | Settings | File Templates.
 */

trait PlatformManagementAdapter {
  
  def listStorages:Array[StorageDescription]

  def listStorageTypes:Array[String]

  def createStorage(stType:String, path:String)

  def removeStorage(id:String)

  def migrate(source:String, target:String)

  def setStorageMode(id:String, mode:String)

  def setPlatformProperty(key:String, value:String)

  def platformProperties:Array[Pair]

  def listTasks:Array[TaskDescription]

  def setTaskMode(taskId:String, mode:String)

  def listTypeMappigs:Array[TypeMapping]

  def addTypeMapping(mimeType:String, storage:String, versioned:java.lang.Short)

  def removeTypeMapping(mimeType:String)

  def listSizeMappings:Array[SizeMapping]

  def addSizeMapping(size:java.lang.Long, storage:String, versioned:java.lang.Integer)

  def removeSizeMapping(size:java.lang.Long)

  def buildResourceList(target:String)
}