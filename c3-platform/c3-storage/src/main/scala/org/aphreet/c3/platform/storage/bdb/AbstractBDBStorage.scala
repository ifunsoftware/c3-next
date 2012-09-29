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

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.resource.Resource

import java.io.File

import org.aphreet.c3.platform.exception.{ResourceNotFoundException, StorageException}
import org.aphreet.c3.platform.storage.{StorageParams, StorageIterator}
import com.sleepycat.je._
import collection.mutable.{HashSet, HashMap}
import org.aphreet.c3.platform.storage.common.AbstractStorage

abstract class AbstractBDBStorage(override val parameters:StorageParams,
                                  override val systemId:String,
                                  val config:BDBConfig) extends AbstractStorage(parameters, systemId)
                                                        with DataManipulator
                                                        with DatabaseProvider
                                                        with FailoverStrategy{


  protected var objectCount:Long = -1

  protected val storageName:String = name + "-" + id

  protected val storagePath:String = path.toString + "/" + storageName

  val iterators = new HashSet[BDBStorageIterator]


  def count:Long = objectCount

  override protected def updateObjectCount() {
    log trace "Updating object count"
    val cnt = getRWDatabase.count
    this.synchronized{
      objectCount = cnt
    }

  }

  def size:Long = calculateSize(new File(storagePath))

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

    if (getRODatabase.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      val resource = Resource.fromByteArray(value.getData)
      resource.address = ra
      loadData(resource)
      Some(resource)
    } else None

  }

  def add(resource:Resource):String = {

    val tx = getEnvironment.beginTransaction(null, null)

    preSave(resource)

    resource.embedData = canEmbedData(resource, config)

    try{
      storeData(resource, tx)

      val key = new DatabaseEntry(resource.address.getBytes)
      val value = new DatabaseEntry(resource.toByteArray)

      failuresArePossible{

        val status = getRWDatabase.putNoOverwrite(tx, key, value)

        if(status != OperationStatus.SUCCESS){
          throw new StorageException("Failed to store resource in database, operation status is: " + status.toString + "; address: " + resource.address)
        }
      }

      tx.commit()

      postSave(resource)

      resource.address
    }catch{
      case e: Throwable => {
        tx.abort()
        throw e
      }
    }
  }

  def put(resource:Resource) {

    val tx = getEnvironment.beginTransaction(null, null)

    try{
      putData(resource, tx)

      val key = new DatabaseEntry(resource.address.getBytes)
      val value = new DatabaseEntry(resource.toByteArray)

      failuresArePossible{
        val status = getRWDatabase.put(tx, key, value)

        if(status != OperationStatus.SUCCESS){
          throw new StorageException("Failed to store resource in database, operation status: " + status.toString)
        }
      }

      tx.commit()
    }catch{
      case e: Throwable => {
        tx.abort()
        throw e
      }
    }
  }

  def delete(ra:String) {
    val key = new DatabaseEntry(ra.getBytes)

    val tx = getEnvironment.beginTransaction(null, null)
    try{
      deleteData(ra, tx)

      failuresArePossible{

        val status = getRWDatabase.delete(tx, key)

        if(status != OperationStatus.SUCCESS)
          throw new StorageException("Failed to delete data from DB, op status: " + status.toString)

      }
      tx.commit()
    }catch{
      case e: Throwable => {
        tx.abort()
        throw e
      }
    }
  }

  def appendSystemMetadata(ra:String, metadata:Map[String, String]){

    val tx = getEnvironment.beginTransaction(null, null)

    try{

      //Obtaining actual version of resource and locking it for write
      val savedResource:Resource = {
        val key = new DatabaseEntry(ra.getBytes)
        val value = new DatabaseEntry()

        val status = getRWDatabase.get(tx, key, value, LockMode.RMW)
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

      failuresArePossible{
        if(getRWDatabase.put(tx, key, value) != OperationStatus.SUCCESS){
          throw new StorageException("Failed to store resource in database")
        }
      }

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

    val tx = getEnvironment.beginTransaction(null, null)


    try {

      //Obtaining actual version of resource and locking it for write
      val savedResource:Resource = {
        val key = new DatabaseEntry(ra.getBytes)
        val value = new DatabaseEntry()

        val status = getRWDatabase.get(tx, key, value, LockMode.RMW)
        if(status == OperationStatus.SUCCESS){
          val res = Resource.fromByteArray(value.getData)
          res.address = ra
          res
        }else throw new ResourceNotFoundException(
          "Failed to get resource with address " + ra + " Operation status " + status.toString)
      }

      //Replacing metadata
      savedResource.metadata.clear()
      savedResource.metadata ++= resource.metadata

      //Appending system metadata
      savedResource.systemMetadata ++= resource.systemMetadata


      for(version <- resource.versions if !version.persisted)
        savedResource.addVersion(version)

      savedResource.embedData = canEmbedData(resource, config)

      storeData(savedResource, tx)

      val key = new DatabaseEntry(ra.getBytes)
      val value = new DatabaseEntry(savedResource.toByteArray)

      failuresArePossible{
        if(getRWDatabase.put(tx, key, value) != OperationStatus.SUCCESS){
          throw new StorageException("Failed to store resource in database")
        }
      }

      tx.commit()

      postSave(savedResource)

      ra
    }catch{
      case e: Throwable => {
        tx.abort()
        throw e
      }
    }
  }



  def iterator(fields:Map[String,String],
               systemFields:Map[String,String],
               filter:(Resource) => Boolean):StorageIterator = {

    if(log.isDebugEnabled){
      log debug "Creating iterator; fields: " + fields + " sysFields: " + systemFields
    }

    val iterator = new BDBStorageIterator(this, fields, systemFields, filter)

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

  def getSecondaryDatabases(writeFlag : Boolean) : HashMap[String, SecondaryDatabase]

  protected def getEnvironment: Environment

}

trait DatabaseProvider{

  def getDatabase(writeFlag : Boolean) : Database

  def getRODatabase:Database = getDatabase(writeFlag = false)

  def getRWDatabase:Database = getDatabase(writeFlag = true)

}

