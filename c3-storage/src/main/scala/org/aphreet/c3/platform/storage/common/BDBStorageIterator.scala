package org.aphreet.c3.platform.storage.common

import com.sleepycat.je._

import org.aphreet.c3.platform.resource.{AddressGenerator, Resource}
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.storage.{StorageIndex, StorageIterator}
import collection.immutable.{HashMap}
import collection.mutable.HashSet
import org.aphreet.c3.platform.common.ComponentGuard
import org.apache.commons.logging.LogFactory

class BDBStorageIterator(val storage: AbstractBDBStorage,
                         val map:Map[String, String],
                         val systemMap:Map[String, String],
                         val filter:Function1[Resource, Boolean]) extends StorageIterator with ComponentGuard{

  val log = LogFactory getLog getClass

  var cursor: Cursor = null

  var joinCursor : JoinCursor = null

  var secCursors : List[SecondaryCursor] = List()

  private val usedIndexes:Map[StorageIndex, String] = indexesForQuery(map, systemMap)

  private val mapFilter:Function1[Resource, Boolean] = createMapFilter(map)
  private val systemMapFilter:Function1[Resource, Boolean] = createSystemMapFilter(systemMap)


  private var bdbEntriesProcessed = 0

  private var resource: Resource = null

  private var useIndexes = false

  private var isEmptyIterator = false

  private var closed = false

  var disableFunctionFilter = false

  {
    useIndexes = !usedIndexes.isEmpty

    if(useIndexes){

      for((index, value) <- usedIndexes){

        val indexDb = storage.getSecondaryDatabases(true).get(index.name) match{
          case Some(db) => db
          case None => throw new StorageException("Failed to open index " + index.name + " database is not open or exist")
        }

        val indexKey = new DatabaseEntry(value.getBytes("UTF-8"))

        val secCursor = indexDb.openCursor(null, null)

        val indexVal = new DatabaseEntry

        val status = secCursor.getSearchKey(indexKey, indexVal, LockMode.DEFAULT)

        if(status == OperationStatus.SUCCESS){
          secCursors = secCursor :: secCursors
        }else{
          isEmptyIterator = true
        }

      }

      //joinCursor = storage.database.join(secCursors.toArray, null)
      joinCursor = storage.getDatabase(true).join(secCursors.toArray, null)


    }else{
      //cursor = storage.database.openCursor(null, null)
      cursor = storage.getDatabase(true).openCursor(null, null)
    }


    resource = findNextResource

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

  private def createMapFilter(map:Map[String, String]):Function1[Resource, Boolean] = {

    val ordIndex = new HashSet[String]

    storage.indexes.map((i:StorageIndex) =>
      if(!i.system){
        ordIndex += i.fields.head
      }
    )

    var ordFields = new HashMap[String,String]() ++ map

    for(field <- ordIndex){
      ordFields = ordFields - field
    }

    if(!ordFields.isEmpty){

      ((res:Resource) => {
        var result = true

        for((key, value) <- ordFields){
          res.metadata.get(key) match{
            case Some(x) => if(x != value) result = false
            case None => result = false
          }
        }

        result
      })

    }else{
      ((res:Resource) => true)
    }

  }

  private def createSystemMapFilter(map:Map[String, String]):Function1[Resource, Boolean] = {
    val sysIndex = new HashSet[String]

    storage.indexes.map((i:StorageIndex) =>
      if(i.system){
        sysIndex += i.fields.head
      }
    )

    var sysFields = new HashMap[String, String]() ++ systemMap

    for(field <- sysIndex){
      sysFields = sysFields - field
    }

    if(!sysFields.isEmpty){

      ((res:Resource) => {
        var result = true

        for((key, value) <- sysFields){
          res.systemMetadata.get(key) match{
            case Some(x) => if(x != value) result = false
            case None => result = false
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

    val previousResource = resource

    resource = findNextResource

    previousResource
  }

  protected def findNextResource: Resource = {

    var resource: Resource = null

    var resultFound = false


    while (!resultFound) {

      if(closed){
        throw new StorageException("Iterator was closed")
      }

      if (!storage.mode.allowRead) {
        this.close
        throw new StorageException("Storage " + storage.id + " is not readable")
      }

      val databaseKey = new DatabaseEntry
      val databaseValue = new DatabaseEntry

      if(!useIndexes){
        if (cursor.getNext(databaseKey, databaseValue, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
          val key = new String(databaseKey.getData)

          if (AddressGenerator.isValidAddress(key)) {
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
          bdbEntriesProcessed = bdbEntriesProcessed + 1;
        } else {
          resource = null
          resultFound = true
        }
      }else{
        if (joinCursor.getNext(databaseKey, databaseValue, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
          val key = new String(databaseKey.getData)

          if (AddressGenerator.isValidAddress(key)) {
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
          bdbEntriesProcessed = bdbEntriesProcessed + 1;
        } else {
          resource = null
          resultFound = true
        }
      }
    }

    resource
  }



  protected def loadData(resource: Resource) = storage.loadData(resource)



  override def objectsProcessed: Int = bdbEntriesProcessed

  def close = {
    try {

      log debug "Closing itertor"

      closed = true


      if(secCursors != null){

        log debug "Closing secondary cursors"

        for(secCursor <- secCursors){
          letItFall{
            secCursor.close
          }
        }
        secCursors = null
      }


      letItFall{
        if(joinCursor != null){
          log debug "Closing joint cursor"
          
          joinCursor.close
          joinCursor = null
        }
      }


      letItFall{
        if (cursor != null) {
          log debug "Closing cursor"

          cursor.close
          cursor = null
        }
      }
    } catch {
      case e: DatabaseException => e.printStackTrace
    } finally {
      storage.removeIterator(this)
      log debug "Iterator closed"
    }
  }

  override def finalize = {
    try {
      if(!closed){
        log debug "Finalize block called, closing iterator"
        this.close
      }
    } catch {
      case e => e.printStackTrace
    }
  }
}
