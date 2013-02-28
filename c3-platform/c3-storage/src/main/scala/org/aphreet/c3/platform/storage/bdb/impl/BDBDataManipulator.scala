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
package org.aphreet.c3.platform.storage.bdb.impl

import com.sleepycat.je._
import org.aphreet.c3.platform.exception.{StorageException, ResourceNotFoundException}
import org.aphreet.c3.platform.resource.{BytesDataStream, Resource, DataStream, ResourceVersion}
import org.aphreet.c3.platform.storage.bdb._
import scala.Some

trait BDBDataManipulator extends DataManipulator with DatabaseProvider{

  override def storeData(resource: Resource, tx: Transaction) {

    if (!resource.embedData){
      if (resource.isVersioned) {
        for (version <- resource.versions) {
          if (version.persisted == false) {
            val versionKey = resource.address + "-data-" + String.valueOf(System.currentTimeMillis) + "-" + version.data.hash
            version.systemMetadata(Resource.MD_DATA_ADDRESS) = versionKey
            storeVersionData(versionKey, version, tx, allowOverwrite = false)
          }
        }
      } else {
        if(!resource.versions(0).persisted){
          val versionKey = resource.address + "-data"
          resource.versions(0).systemMetadata(Resource.MD_DATA_ADDRESS) = versionKey
          storeVersionData(versionKey, resource.versions(0), tx, allowOverwrite = true)
        }
      }
    }
  }

  def loadData(resource: Resource) {

    if (!resource.embedData){

      for (version <- resource.versions) {

        val versionKey = version.systemMetadata(Resource.MD_DATA_ADDRESS) match {
          case Some(value: String) => value
          case None => throw new StorageException("Can't find data reference for version in resource: " + resource.address)
        }

        version.data = new LazyBDBDataStream(versionKey, roDatabase)
      }
    }
  }


  def loadDataForUpdate(resource: Resource, tx: Transaction) {
    if (!resource.embedData){

      for (version <- resource.versions) {

        val versionKey = version.systemMetadata(Resource.MD_DATA_ADDRESS) match {
          case Some(value: String) => value
          case None => throw new StorageException("Can't find data reference for version in resource: " + resource.address)
        }

        val key = new DatabaseEntry(versionKey.getBytes("UTF-8"))
        val value = new DatabaseEntry()

        if(rwDatabase.get(tx, key, value, LockMode.RMW) == OperationStatus.SUCCESS){
          version.data = new BytesDataStream(value.getData)
        }else{
          throw new StorageException("Can't load data for update")
        }
      }
    }
  }

  override def deleteData(ra: String, tx: Transaction) {

    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    val status = rwDatabase.get(null, key, value, LockMode.DEFAULT)

    if (status == OperationStatus.SUCCESS) {
      val resource = Resource.fromByteArray(value.getData)

      if (!resource.embedData){

        for (version <- resource.versions) {
          val dataKey = version.systemMetadata(Resource.MD_DATA_ADDRESS) match {
            case Some(address) => new DatabaseEntry(address.getBytes)
            case None => throw new StorageException("No data address in version for resource: " + ra)
          }
          rwDatabase.delete(tx, dataKey)
        }
      }


    } else throw new ResourceNotFoundException(ra)

  }

  private def storeVersionData(key: String, version: ResourceVersion, tx: Transaction, allowOverwrite: Boolean) {

    val dbKey = new DatabaseEntry(key.getBytes)
    val dbValue = new DatabaseEntry(version.data.getBytes)

    var status: OperationStatus = null

    if (allowOverwrite) {
      status = rwDatabase.put(tx, dbKey, dbValue)
    } else {
      status = rwDatabase.putNoOverwrite(tx, dbKey, dbValue)
    }

    if (status != OperationStatus.SUCCESS) {
      throw new StorageException("Failed to write version data, operation status is " + status.toString)
    }

    version.data = DataStream.create(version.data.getBytes)
    version.systemMetadata(Resource.MD_DATA_LENGTH) = version.data.length
  }

  override
  def canEmbedData(resource:Resource, config:BDBConfig):Boolean = {
    if(resource.isVersioned)  false
    else resource.versions(0).data.length < config.embedThreshold
  }

}