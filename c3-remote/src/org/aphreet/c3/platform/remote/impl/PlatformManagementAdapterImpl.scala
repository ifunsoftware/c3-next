package org.aphreet.c3.platform.remote.impl

import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.remote.api.management.{Pair, SizeMapping, TypeMapping, StorageDescription, PlatformManagementAdapter, TaskDescription => RemoteTaskDescription}
import org.aphreet.c3.platform.exception.PlatformException
import org.aphreet.c3.platform.task.{RUNNING, PAUSED, TaskState, TaskDescription}
import org.aphreet.c3.platform.storage.{StorageException, U, RO, RW}
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.remote.api.RemoteException
import scala.collection.jcl.Map

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 1:44:09 PM
 * To change this template use File | Settings | File Templates.
 */

@Component("platformManagementAdapter")
class PlatformManagementAdapterImpl extends PlatformManagementAdapter{
  val log = LogFactory getLog getClass

  var managementEndpoint:PlatformManagementEndpoint = null

  @Autowired
  def setManagementEndpoint(endPoint:PlatformManagementEndpoint)
  = {managementEndpoint = endPoint}

  def listStorages:Array[StorageDescription] = {

    catchAll(() => {
      (for(s <-managementEndpoint.listStorages)
        yield new StorageDescription(s.id,
            s.getClass.getSimpleName,
            s.path.toString,
            s.mode.name + "(" + s.mode.message + ")",
            s.count)).toArray
    })

  }

  def listStorageTypes:Array[String] =
    catchAll(() => {
      managementEndpoint.listStorageTypes.toArray
    })


  def createStorage(stType:String, path:String) =
    catchAll(() => {managementEndpoint.createStorage(stType, path)})

  def removeStorage(id:String) =
    catchAll(() => {managementEndpoint.removeStorage(id)})

  def migrate(source:String, target:String) =
    catchAll(() => {
      managementEndpoint.migrateFromStorageToStorage(source, target)
    })

  def setStorageMode(id:String, mode:String) =
    catchAll(() => {
      val storageMode = mode match {
        case "RW" => RW(STORAGE_MODE_USER)
        case "RO" => RO(STORAGE_MODE_USER)
        case "U" => U(STORAGE_MODE_USER)
        case _ => throw new StorageException("No mode named " + mode)
      }
      managementEndpoint.setStorageMode(id, storageMode)
    })


  def setPlatformProperty(key:String, value:String) =
    catchAll(() =>
      managementEndpoint.setPlatformProperty(key, value)
      )

  def platformProperties:Array[Pair] =
    catchAll(() => {
      (for(e <- Map.apply(managementEndpoint.getPlatformProperties))
        yield new Pair(e._1, e._2)).toSeq.toArray
    })

  def listTasks:Array[RemoteTaskDescription] =
    catchAll(() =>
      managementEndpoint.listTasks.map(fromLocalDescription(_)).toArray
    )

  private def fromLocalDescription(descr:TaskDescription):RemoteTaskDescription = {
    new RemoteTaskDescription(descr.id, descr.name, descr.state.name, descr.progress.toString)
  }

  def setTaskMode(taskId:String, mode:String) =
    catchAll(() => {
      val state:TaskState = mode match {
        case "pause" => PAUSED
        case "resume"   => RUNNING
        case _ => throw new PlatformException("mode is not valid")

      }

      managementEndpoint.setTaskMode(taskId, state)
    })

  def listTypeMappigs:Array[TypeMapping] =
    catchAll(() => {
      (for(entry <- managementEndpoint.listTypeMappings)
        yield new TypeMapping(entry._1, entry._2, if(entry._3) 1.shortValue else 0.shortValue )).toArray
    })

  def addTypeMapping(mimeType:String, storage:String, versioned:java.lang.Short) =
    catchAll(() => {
      val vers = versioned == 1
      managementEndpoint.addTypeMapping((mimeType, storage, vers))
    })

  def removeTypeMapping(mimeType:String) =
    catchAll(() =>
      managementEndpoint.removeTypeMapping(mimeType)
      )

  def listSizeMappings:Array[SizeMapping] =
    catchAll(() => {
      (for(entry <- managementEndpoint.listSizeMappings)
        yield new SizeMapping(entry._1, entry._2, if(entry._3) 1 else 0)).toArray
    })

  def addSizeMapping(size:java.lang.Long, storage:String, versioned:java.lang.Integer) =
    catchAll(() => {
      val vers = versioned == 1
      managementEndpoint.addSizeMapping((size.longValue, storage, vers))
    })

  def removeSizeMapping(size:java.lang.Long) =
    catchAll(() => {
      managementEndpoint.removeSizeMaping(size.longValue)
    })

  private def catchAll[T](function:Function0[T]) : T = {
    try{
      function.apply
    }catch{
      case e => {
        log.error(e)
        throw new RemoteException("Exception " + e.getClass.getCanonicalName + ": " + e.getMessage)
      }
    }
  }
}