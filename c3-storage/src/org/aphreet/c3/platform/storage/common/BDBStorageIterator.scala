package org.aphreet.c3.platform.storage.common

import com.sleepycat.je._

import org.aphreet.c3.platform.resource.{AddressGenerator, Resource}
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.storage.{StorageIndex, StorageIterator}
import collection.immutable.{HashMap}
import collection.mutable.HashSet

class BDBStorageIterator(val storage: AbstractBDBStorage,
                         val map:Map[String, String],
                         val systemMap:Map[String, String],
                         val filter:Function1[Resource, Boolean]) extends StorageIterator {

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

  var disableFunctionFilter = false

  {
    useIndexes = !usedIndexes.isEmpty

    if(useIndexes){

      for((index, value) <- usedIndexes){

        val indexDb = storage.secondaryDatabases.get(index.name) match{
          case Some(db) => db
          case None => throw new StorageException("Failed to open index " + index.name + " database is not open or exist")
        }

        val indexKey = new DatabaseEntry(value.getBytes("UTF-8"))

        val secCursor = indexDb.openSecondaryCursor(null, null)

        val indexVal = new DatabaseEntry

        val status = secCursor.getSearchKey(indexKey, indexVal, LockMode.DEFAULT)

        if(status == OperationStatus.SUCCESS){
          secCursors = secCursor :: secCursors
        }else{
          isEmptyIterator = true
        }

      }

      joinCursor = storage.database.join(secCursors.toArray, null)



    }else{
      cursor = storage.database.openCursor(null, null)
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

      if(secCursors != null){
        for(secCursor <- secCursors){
          secCursor.close
        }
        secCursors = null
      }

      if(joinCursor != null){
        joinCursor.close
        joinCursor = null
      }


      if (cursor != null) {
        cursor.close
        cursor = null
      }
    } catch {
      case e: DatabaseException => e.printStackTrace
    }
  }

  override def finalize = {
    try {
      this.close
    } catch {
      case e => e.printStackTrace
    }
  }
}
