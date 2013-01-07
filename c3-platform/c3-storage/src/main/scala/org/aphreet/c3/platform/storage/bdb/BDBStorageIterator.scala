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
package org.aphreet.c3.platform.storage.bdb

import com.sleepycat.je._

import org.aphreet.c3.platform.resource.{ResourceAddress, Resource}
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.storage.{StorageIndex, StorageIterator}
import collection.immutable.HashMap
import org.aphreet.c3.platform.common.ComponentGuard
import org.apache.commons.logging.LogFactory

class BDBStorageIterator(val storage: AbstractBDBStorage,
                         val map:Map[String, String],
                         val systemMap:Map[String, String],
                         val filter:(Resource) => Boolean,
                         val disableFunctionFilter: Boolean) extends StorageIterator with ComponentGuard{

  val log = LogFactory getLog getClass

  var cursor: Cursor = null

  var joinCursor : JoinCursor = null

  var secCursors : List[SecondaryCursor] = List()

  private val usedIndexes:Map[StorageIndex, String] = indexesForQuery(map, systemMap)

  private val mapFilter:(Resource) => Boolean = createMapFilter(map)
  private val systemMapFilter:(Resource) => Boolean = createSystemMapFilter(systemMap)


  private var bdbEntriesProcessed = 0

  private var resource: Resource = null

  private var useIndexes = false

  private var isEmptyIterator = false

  private var closed = false

  {

    log.debug("Creating storage iterator for " + storage.id)

    useIndexes = !usedIndexes.isEmpty

    if(useIndexes){

      if(log.isDebugEnabled){
        log.debug("Using indexes " + usedIndexes.toString())
        log.debug("Opening secondary cursors")
      }

      for((index, value) <- usedIndexes){

        val indexDb = storage.getSecondaryDatabases(writeFlag = true).get(index.name) match{
          case Some(db) => db
          case None => throw new StorageException("Failed to open index " + index.name + " database is not open or exist")
        }


        val secCursor = indexDb.openCursor(null, null)

        val indexKey = new DatabaseEntry(value.getBytes("UTF-8"))
        val indexVal = new DatabaseEntry
        val status = secCursor.getSearchKey(indexKey, indexVal, LockMode.DEFAULT)


        if(status == OperationStatus.SUCCESS){
          secCursors = secCursor :: secCursors
        }else{
          log.debug("failed to open cursor in storage " + storage.id + " for index: " + index + " and value " + value + ", operation status is " + status.toString)
          isEmptyIterator = true

          secCursor.close()
        }

      }

      if(!isEmptyIterator){
        joinCursor = storage.getRWDatabase.join(secCursors.toArray, null)
      }

    }else{
      log.debug("No indexes can be used for specified query")
      cursor = storage.getRWDatabase.openCursor(null, null)
    }

    if(!isEmptyIterator){
      resource = findNextResource
    }

  }

  private def indexesForQuery(query:Map[String, String],
                              systemMap:Map[String, String]
                               ):Map[StorageIndex, String] = {

    var map = new HashMap[StorageIndex, String]

    for(index <- storage.indexes){

      if(!index.system){

        if(query.contains(index.fields.head)){
          val e:(StorageIndex, String) = (index, query.get(index.fields.head).get)
          map = map + e
        }

      }else{
        if(systemMap.contains(index.fields.head)){
          val e:(StorageIndex, String) = (index, systemMap.get(index.fields.head).get)
          map = map + e
        }
      }
    }

    map
  }

  private def createMapFilter(map:Map[String, String]):(Resource) => Boolean = {

    log.debug("Creading filter function for user metadata")

    //TODO rewrite last map call if we ever will have multi-field indexes
    val indexedFields = storage.indexes.filter(!_.system).map(_.fields.head).toSet

    val fieldsToCheck = map -- indexedFields

    log.debug("Function will check the following user md fields: " + fieldsToCheck)

    if(!fieldsToCheck.isEmpty){

      ((res:Resource) => {
        var result = true

        for((key, value) <- fieldsToCheck){
          res.metadata.get(key) match{
            case Some(x) =>
              if(x != value) result = false
            case None =>
              result = false
          }
        }

        result
      })

    }else{
      ((res:Resource) => true)
    }

  }

  private def createSystemMapFilter(map:Map[String, String]):(Resource) => Boolean = {

    log.debug("Creading filter function for system metadata")

    //TODO rewrite last map call if we ever will have multi-field indexes
    val indexedFields = storage.indexes.filter(_.system).map(_.fields.head).toSet

    val fieldsToCheck = map -- indexedFields

    log.debug("Function will check the following sys md fields: " + fieldsToCheck)

    if(!fieldsToCheck.isEmpty){

      ((res:Resource) => {
        var result = true

        for((key, value) <- fieldsToCheck){
          res.systemMetadata.get(key) match{
            case Some(x) =>
              if(x != value) result = false
            case None =>
              result = false
          }
        }

        result
      })

    }else{
      ((res:Resource) => true)
    }
  }

  def isMatch(resource:Resource):Boolean = {
    filter(resource) && mapFilter(resource) && systemMapFilter(resource)
  }


  def hasNext: Boolean = {
    !isEmptyIterator && resource != null
  }

  def next: Resource = {

    if(hasNext){

      val previousResource = resource

      resource = findNextResource

      previousResource
    }else{
      null
    }
  }

  protected def findNextResource: Resource = {

    var resource: Resource = null
    var resultFound = false

    while (!resultFound) {

      if(closed){
        throw new StorageException("Iterator was closed")
      }

      if (!storage.mode.allowRead) {
        this.close()
        throw new StorageException("Storage " + storage.id + " is not readable")
      }

      val databaseKey = new DatabaseEntry
      val databaseValue = new DatabaseEntry

      if(fetchNextKey(databaseKey, databaseValue) == OperationStatus.SUCCESS){
        val key = new String(databaseKey.getData)

        if (ResourceAddress.isValidAddress(key)) {

          if(fetchCurrentValue(databaseKey, databaseValue) == OperationStatus.SUCCESS){

            resource = Resource.fromByteArray(databaseValue.getData)

            if(disableFunctionFilter){
              loadData(resource)
              resultFound = true
            }else{
              if(isMatch(resource)){
                loadData(resource)
                resultFound = true
              }
            }
          }
        }

        bdbEntriesProcessed = bdbEntriesProcessed + 1
      }else{
        resource = null
        resultFound = true
        this.close()
      }
    }

    resource
  }

  private def fetchNextKey(dbKey:DatabaseEntry, dbValue:DatabaseEntry): OperationStatus = {
    if(useIndexes){
      joinCursor.getNext(dbKey, LockMode.DEFAULT)
    }else{
      dbValue.setPartial(0, 0, true)
      cursor.getNext(dbKey, dbValue, LockMode.DEFAULT)
    }
  }

  private def fetchCurrentValue(dbKey:DatabaseEntry, dbValue:DatabaseEntry):OperationStatus = {
    if(useIndexes){
      joinCursor.getDatabase.get(null, dbKey, dbValue, LockMode.DEFAULT)
    }else{
      dbValue.setPartial(false)
      cursor.getCurrent(dbKey, dbValue, LockMode.DEFAULT)
    }
  }

  protected def loadData(resource: Resource) {
    storage.loadData(resource)
  }

  override def objectsProcessed: Int = bdbEntriesProcessed

  def close() {
    if(!closed){
      try {

        log debug "Closing itertor"

        closed = true

        if(secCursors != null){

          log debug "Closing secondary cursors"

          for(secCursor <- secCursors){
            letItFall{
              secCursor.close()
            }
          }
          secCursors = null
        }


        letItFall{
          if(joinCursor != null){
            log debug "Closing joint cursor"

            joinCursor.close()
            joinCursor = null
          }
        }


        letItFall{
          if (cursor != null) {
            log debug "Closing cursor"

            cursor.close()
            cursor = null
          }
        }
      } catch {
        case e: DatabaseException => e.printStackTrace()
      } finally {
        storage.removeIterator(this)
        log debug "Iterator closed"
      }
    }
  }

  override def finalize() {
    try {
      if(!closed){
        log debug "Finalize block called, closing iterator"
        this.close()
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
}
