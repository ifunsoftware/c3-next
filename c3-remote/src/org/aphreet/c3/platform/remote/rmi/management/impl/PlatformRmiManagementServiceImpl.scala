package org.aphreet.c3.platform.remote.rmi.management.impl

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import org.aphreet.c3.platform.remote.api.rmi.management.PlatformRmiManagementService
import org.aphreet.c3.platform.remote.api.management._

@Component("platformRmiManagementService")
class PlatformRmiManagementServiceImpl extends PlatformRmiManagementService{

  var managementAdapter:PlatformManagementAdapter = null

  @Autowired
  def setManagementAdapter(adapter:PlatformManagementAdapter) = {managementAdapter = adapter}

  def listStorages:Array[StorageDescription] = managementAdapter.listStorages

  def listStorageTypes:Array[String] = managementAdapter.listStorageTypes

  def createStorage(stType:String, path:String) = managementAdapter.createStorage(stType, path)

  def removeStorage(id:String) = managementAdapter.removeStorage(id)

  def migrate(source:String, target:String) = managementAdapter.migrate(source, target)

  def setStorageMode(id:String, mode:String) = managementAdapter.setStorageMode(id, mode)

  def setPlatformProperty(key:String, value:String) = managementAdapter.setPlatformProperty(key, value)

  def platformProperties:Array[Pair] = managementAdapter.platformProperties

  def listTasks:Array[TaskDescription] = managementAdapter.listTasks

  def listFinishedTasks:Array[TaskDescription] = managementAdapter.listFinishedTasks

  def setTaskMode(taskId:String, mode:String) = managementAdapter.setTaskMode(taskId, mode)

  def listTypeMappigs:Array[TypeMapping] = managementAdapter.listTypeMappigs

  def addTypeMapping(mimeType:String, storage:String, versioned:java.lang.Short)
    = managementAdapter.addTypeMapping(mimeType, storage, versioned)

  def removeTypeMapping(mimeType:String) = managementAdapter.removeTypeMapping(mimeType)

  def listSizeMappings:Array[SizeMapping] = managementAdapter.listSizeMappings

  def addSizeMapping(size:java.lang.Long, storage:String, versioned:java.lang.Integer)
    = managementAdapter.addSizeMapping(size, storage, versioned)

  def removeSizeMapping(size:java.lang.Long) = managementAdapter.removeSizeMapping(size)


  def listUsers:Array[Pair] = managementAdapter.listUsers

  def addUser(name:String, password:String, role:String) = managementAdapter.addUser(name,password,role)

  def updateUser(name:String, password:String, role:String) = managementAdapter.updateUser(name, password, role)

  def deleteUser(name:String) = managementAdapter.deleteUser(name)


  def statistics:Array[Pair] = managementAdapter.statistics
}

