/**
 * Copyright (c) 2010, Mikhail Malygin, Anton Krasikov
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

package org.aphreet.c3.platform.storage.bdb

import com.sleepycat.je._
import java.io.File
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.exception.{ResourceNotFoundException, StorageException}
import org.aphreet.c3.platform.resource.{ResourceVersion, Resource}
import org.aphreet.c3.platform.storage.common.AbstractStorage
import org.aphreet.c3.platform.storage.{ConflictResolverProvider, StorageParams, StorageIterator}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import java.nio.file.Files

abstract class AbstractBDBStorage(override val parameters:StorageParams,
                                  override val systemId:String,
                                  val config:BDBConfig,
                                  val conflictResolverProvider: ConflictResolverProvider) extends AbstractStorage(parameters, systemId)
with DataManipulator
with DatabaseProvider{


  protected var objectCount:Long = -1

  protected val storageName:String = name + "-" + id

  protected val storagePath:String = path.toString + "/" + storageName

  val iterators = new mutable.HashSet[BDBStorageIterator]

  val disableIteratorFunctionFilter = parameters.params.contains("DISABLE_BDB_FUNCTION_FILTER")

  def count:Long = objectCount

  override protected def updateObjectCount() {
    log trace "Updating object count"
    val cnt = rwDatabase.count

    this.synchronized{
      objectCount = cnt
    }

  }

  def availableCapacity = Files.getFileStore(new File(storagePath).toPath).getUsableSpace

  def usedCapacity:Long = calculateSize(new File(storagePath))

  def fullPath:Path = new Path(storagePath)


  private def calculateSize(dir:File):Long = {
    var size:Long = 0

    for(child <- dir.listFiles){
      if(!child.isDirectory){
        size = size + child.length
      }else{
        size = size + calculateSize(child)
      }
    }

    size
  }

  def get(ra:String):Option[Resource] = {

    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    if (roDatabase.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      val resource = Resource.fromByteArray(value.getData)
      resource.address = ra
      loadData(resource)
      Some(resource)
    } else None

  }

  def add(resource:Resource):String = {

    val tx = environment.beginTransaction(null, null)

    preSave(resource)

    resource.embedData = canEmbedData(resource, config)

    try{
      storeData(resource, tx)

      val key = new DatabaseEntry(resource.address.getBytes)
      val value = new DatabaseEntry(resource.toByteArray)

      val status = rwDatabase.putNoOverwrite(tx, key, value)

      if(status != OperationStatus.SUCCESS){
        throw new StorageException("Failed to store resource in database, operation status is: " + status.toString + "; address: " + resource.address)
      }

      tx.commit()

      dataLog.debug("Added resource " + resource.address)

      postSave(resource)

      resource.address
    }catch{
      case e: Throwable => {
        tx.abort()
        throw e
      }
    }
  }

  def delete(ra:String) {
    val key = new DatabaseEntry(ra.getBytes)

    val tx = environment.beginTransaction(null, null)
    try{
      deleteData(ra, tx)

      val status = rwDatabase.delete(tx, key)

      if(status != OperationStatus.SUCCESS)
        throw new StorageException("Failed to delete data from DB, op status: " + status.toString)

      tx.commit()

      dataLog.debug("Deleted resource " + ra)

    }catch{
      case e: Throwable => {
        tx.abort()
        throw e
      }
    }
  }

  def appendSystemMetadata(ra:String, metadata:Map[String, String]){

    val tx = environment.beginTransaction(null, null)

    try{

      //Obtaining actual version of resource and locking it for write
      val savedResource:Resource = {
        val key = new DatabaseEntry(ra.getBytes)
        val value = new DatabaseEntry()

        val status = rwDatabase.get(tx, key, value, LockMode.RMW)
        if(status == OperationStatus.SUCCESS){
          val res = Resource.fromByteArray(value.getData)
          res.address = ra
          res
        }else throw new ResourceNotFoundException(
          "Failed to get resource with address " + ra + " Operation status " + status.toString)
      }
      //Appending system metadata
      savedResource.systemMetadata ++= metadata

      val key = new DatabaseEntry(ra.getBytes)
      val value = new DatabaseEntry(savedResource.toByteArray)

      if(rwDatabase.put(tx, key, value) != OperationStatus.SUCCESS){
        throw new StorageException("Failed to store resource in database")
      }

      dataLog.debug("Updated system meta on " + ra)

      tx.commit()
    }catch{
      case e: Throwable => {
        tx.abort()
        throw e
      }
    }
  }

  def update(resource:Resource):String = {
    val ra = resource.address

    preSave(resource)

    val tx = environment.beginTransaction(null, null)

    try {
      //Obtaining actual version of resource and locking it for write

      val key = new DatabaseEntry(ra.getBytes)
      val value = new DatabaseEntry()

      val status = rwDatabase.get(tx, key, value, LockMode.RMW)
      status match {
        case OperationStatus.SUCCESS => doUpdate(tx, resource, Resource.fromByteArray(value.getData))
        case OperationStatus.NOTFOUND => doPut(tx, resource)
        case _ => throw new ResourceNotFoundException("Failed to update resource with address " + ra + " Operation status " + status.toString)
      }

      ra
    }catch{
      case e: Throwable => {
        tx.abort()
        throw e
      }
    }
  }

  protected def doUpdate(tx: Transaction, resource: Resource, savedResource: Resource){
    //Appending metadata
    savedResource.metadata ++= resource.metadata
    resource.metadata.removed.foreach(savedResource.metadata.remove(_))

    //Appending system metadata
    savedResource.systemMetadata ++= resource.systemMetadata

    if(resource.isVersioned){
      mergeVersions(savedResource, resource)
    }else{

      if (resource.versions.last.basedOnVersion == savedResource.versions.last.date.getTime){
        savedResource.addVersion(resource.versions.last)
      }else{
        loadDataForUpdate(savedResource, tx)
        conflictResolverProvider.conflictResolverFor(resource).resolve(savedResource, resource)
      }

      savedResource.versions.last.persisted = false
    }

    savedResource.embedData = canEmbedData(resource, config)

    storeData(savedResource, tx)

    val key = new DatabaseEntry(resource.address.getBytes)
    val value = new DatabaseEntry(savedResource.toByteArray)

    if(rwDatabase.put(tx, key, value) != OperationStatus.SUCCESS){
      throw new StorageException("Failed to store resource in database")
    }

    tx.commit()

    dataLog.debug("Updated resource " + resource.address)

    postSave(savedResource)
  }

  protected def mergeVersions(savedResource: Resource, incomeResource: Resource) = {

    val lastVersion = incomeResource.versions.last

    //Trivial case
    if (!lastVersion.persisted && lastVersion.basedOnVersion == savedResource.versions.last.date.getTime){
      savedResource.addVersion(lastVersion)
    }else{
      //This is not optimal, however in most cases we'll have
      //a trivial merge scenario where only one resource is added
      val incomeVersions = incomeResource.versions.filter(!_.persisted).reverseIterator

      val savedVersions = savedResource.versions

      savedVersions ++= incomeVersions

      val sortedVersions = savedVersions.sortBy(_.date.getTime)

      var previousVersion: ResourceVersion = null

      val mergedVersions = new ArrayBuffer[ResourceVersion]()

      for (version <- sortedVersions){
        if(previousVersion != null){

          if (version.date.getTime != previousVersion.date.getTime){
            previousVersion = version
            mergedVersions += version
          }else{
            if (version.systemMetadata(ResourceVersion.RESOURCE_VERSION_HASH)
            != previousVersion.systemMetadata(ResourceVersion.RESOURCE_VERSION_HASH)){
              previousVersion = version
              mergedVersions += version
            }
          }

        }else{
          previousVersion = version
          mergedVersions += version
        }
      }

      savedResource.versions.clear()
      savedResource.versions ++= mergedVersions
    }
  }

  protected def doPut(tx:Transaction, resource: Resource){

    resource.embedData = canEmbedData(resource, config)

    resource.versions.foreach(_.persisted = false)

    storeData(resource, tx)

    val key = new DatabaseEntry(resource.address.getBytes)
    val value = new DatabaseEntry(resource.toByteArray)

    val status = rwDatabase.put(tx, key, value)

    if(status != OperationStatus.SUCCESS){
      throw new StorageException("Failed to store resource in database, operation status: " + status.toString)
    }

    tx.commit()

    dataLog.debug("Put resource " + resource.address)

    postSave(resource)
  }



  def iterator(fields:Map[String,String],
               systemFields:Map[String,String],
               filter:(Resource) => Boolean):StorageIterator = {

    if(log.isDebugEnabled){
      log debug "Creating iterator; fields: " + fields + " sysFields: " + systemFields
    }

    val iterator = new BDBStorageIterator(this, fields, systemFields, filter, disableIteratorFunctionFilter)

    iterators.synchronized(
      iterators += iterator
    )

    log debug "Iterator created"

    iterator

  }

  def removeIterator(iterator:BDBStorageIterator){
    iterators.synchronized(
      iterators -= iterator
    )
  }

  protected def preSave(resource:Resource){}

  protected def postSave(resource:Resource){
    for(version <- resource.versions if !version.persisted){
      version.persisted = true
    }
  }

  def secondaryDatabases(writeFlag : Boolean) : mutable.HashMap[String, SecondaryDatabase]

  protected def environment: Environment

}

trait DatabaseProvider{

  def getDatabase(writeFlag : Boolean) : Database

  def roDatabase:Database = getDatabase(writeFlag = false)

  def rwDatabase:Database = getDatabase(writeFlag = true)

}

object AbstractBDBStorage {

  val DISABLE_BDB_FUNCTION_FILTER = "DISABLE_BDB_FUNCTION_FILTER"
  val USE_SHORT_LOCK_TIMEOUT = "USE_SHORT_LOCK_TIMEOUT"

}

