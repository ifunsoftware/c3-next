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
import org.aphreet.c3.platform.common.{Logger, ComponentGuard}
import BDBStorageIndex._

class BDBStorageIterator(val storage: AbstractBDBStorage,
                         val userMeta:Map[String, String],
                         val systemMeta:Map[String, String],
                         val filter:(Resource) => Boolean,
                         val disableFunctionFilter: Boolean) extends StorageIterator with ComponentGuard{

  val log = Logger(getClass)

  var cursor: Cursor = null
  var joinCursor : JoinCursor = null
  var joinConfig: JoinConfig = null
  var secCursors : List[SecondaryCursor] = List()
  var rangedCursor : SecondaryCursor = null
  var hasRangedCursor: Boolean = false

  private val usedIndexes:Map[StorageIndex, String] = indexesForQuery(userMeta, systemMeta)
  private val useIndexes = !usedIndexes.isEmpty

  private val userMetaFilter:(Resource) => Boolean = createUserMetaFilter(userMeta)
  private val systemMetaFilter:(Resource) => Boolean = createSystemMetaFilter(systemMeta)

  private var bdbEntriesProcessed = 0

  private var resource: Resource = null
  private var isEmptyIterator = false
  private var closed = false

  {
    log.debug("Creating storage iterator for " + storage.id)

    if(useIndexes){

      if(log.isDebugEnabled){
        log.debug("Using indexes " + usedIndexes.toString())
        log.debug("Opening secondary cursors")
      }

      for((index, value) <- usedIndexes){

        val secCursor = storage.secondaryDatabases(writeFlag = true).get(index.name) match{
          case Some(db) => db.openCursor(null, null)
          case None => throw new StorageException("Failed to open index " + index.name + " database is not open or exist")
        }

        if(positionSecondaryCursor(secCursor, index, value) == OperationStatus.SUCCESS){
          secCursors = secCursor :: secCursors
        }else{
          log.debug("failed to open cursor in storage " + storage.id + " for index: " + index + " and value " + value + ", operation status is not success")

          isEmptyIterator = true
          secCursor.close()
        }
      }

      if(!isEmptyIterator){

        if(hasRangedCursor){
          secCursors = rangedCursor :: secCursors.filter(_ ne rangedCursor)
        }

        joinConfig = new JoinConfig
        joinConfig.setNoSort(hasRangedCursor)
        joinCursor = storage.rwDatabase.join(secCursors.toArray, joinConfig)
      }

    }else{
      log.debug("No indexes can be used for specified query")
      cursor = storage.rwDatabase.openCursor(null, null)
    }

    if(!isEmptyIterator){
      resource = findNextResource()
    }
  }

  private def positionSecondaryCursor(cursor: SecondaryCursor, index: StorageIndex, value: String): OperationStatus = {
    val indexKey = new DatabaseEntry()
    val rangedQuery = index.putSearchKey(value, indexKey)

    if(rangedQuery){
      rangedCursor = cursor
      hasRangedCursor = true
      cursor.getSearchKeyRange(indexKey, new DatabaseEntry, LockMode.DEFAULT)
    }else{
      cursor.getSearchKey(indexKey, new DatabaseEntry, LockMode.DEFAULT)
    }
  }

  private def indexesForQuery(userMeta: Map[String, String],
                              systemMeta: Map[String, String]
                               ):Map[StorageIndex, String] = {

    (for(index <- storage.indexes)
    yield (if (index.system) systemMeta else userMeta).get(index.fields.head) match {
        case Some(value) => ((index, value))
        case None => ((index, null))
      }).filter(_._2 != null).toMap

  }

  private def createUserMetaFilter(map:Map[String, String]):(Resource) => Boolean =
    createMetadataFilter(map, isSystem = false)

  private def createSystemMetaFilter(map:Map[String, String]):(Resource) => Boolean =
    createMetadataFilter(map, isSystem = true)

  private def createMetadataFilter(map: Map[String, String], isSystem: Boolean): (Resource) => Boolean = {

    log.debug("Creading filter function for system metadata")

    //TODO rewrite last map call if we ever will have multi-field indexes
    val indexedFields = storage.indexes.filter(_.system == isSystem).map(_.fields.head).toSet

    val fieldsToCheck = map -- indexedFields

    log.debug("Function will check the following sys md fields: " + fieldsToCheck)

    if(!fieldsToCheck.isEmpty){

      ((res:Resource) => {
        var result = true

        for((key, value) <- fieldsToCheck){
          (if(isSystem) res.systemMetadata else res.metadata)(key) match{
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

  private def matchesFilter(resource:Resource):Boolean = {
    filter(resource) && userMetaFilter(resource) && systemMetaFilter(resource)
  }


  def hasNext: Boolean = {
    !isEmptyIterator && resource != null
  }

  def next(): Resource = {
    if(hasNext){
      val previousResource = resource
      resource = findNextResource()
      previousResource
    }else{
      null
    }
  }

  protected def findNextResource(): Resource = {

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
              if(matchesFilter(resource)){
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

  private def fetchNextKey(dbKey: DatabaseEntry, dbValue:DatabaseEntry): OperationStatus = {

    def next(cursor:JoinCursor, key: DatabaseEntry): Boolean = {
      joinCursor.getNext(dbKey, LockMode.DEFAULT) == OperationStatus.SUCCESS
    }

    if(useIndexes){

      if (next(joinCursor, dbKey)){
        OperationStatus.SUCCESS
      }else{

        var result = OperationStatus.KEYEMPTY

        while (result == OperationStatus.KEYEMPTY){
          if (moveRangedCursor()){

            joinCursor.close()
            joinCursor = storage.rwDatabase.join(secCursors.toArray, joinConfig)

            if (next(joinCursor, dbKey)){
              result = OperationStatus.SUCCESS
            }
          }else{
            result = OperationStatus.NOTFOUND
          }
        }

        result
      }
    }else{
      //Just fetch a key without value
      dbValue.setPartial(0, 0, true)
      cursor.getNext(dbKey, dbValue, LockMode.DEFAULT)
    }
  }

  private def moveRangedCursor(): Boolean = {
    if(hasRangedCursor && rangedCursor.getNext(new DatabaseEntry(), new DatabaseEntry(), LockMode.DEFAULT) == OperationStatus.SUCCESS){
      true
    }else{
      false
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
