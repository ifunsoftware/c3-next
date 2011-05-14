package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.storage.StorageParams
import org.aphreet.c3.platform.storage.common.{AbstractReplicatedBDBStorage, BDBConfig}
import com.sleepycat.je._
import org.aphreet.c3.platform.exception.{StorageException, ResourceNotFoundException}
import org.aphreet.c3.platform.resource.{Resource, DataWrapper, ResourceVersion}

/**
 * Created by IntelliJ IDEA.
 * User: antey
 * Date: 30.04.11
 * Time: 16:56
 * To change this template use File | Settings | File Templates.
 */

class ReplicatedBDBStorage(override val parameters: StorageParams,
                                    override val systemId:String,
                                    override val config: BDBConfig)
                    extends AbstractReplicatedBDBStorage(parameters, systemId, config) {

  override protected def storeData(resource: Resource, tx: Transaction) {

    if (resource.isVersioned) {
      for (version <- resource.versions) {
        if (version.persisted == false) {
          val versionKey = resource.address + "-data-" + String.valueOf(System.currentTimeMillis) + "-" + version.data.hash
          version.systemMetadata.put(Resource.MD_DATA_ADDRESS, versionKey)
          storeVersionData(versionKey, version, tx, false)
        }
      }
    } else {
      if(!resource.versions(0).persisted){
        val versionKey = resource.address + "-data"
        resource.versions(0).systemMetadata.put(Resource.MD_DATA_ADDRESS, versionKey)
        storeVersionData(versionKey, resource.versions(0), tx, true)
      }
    }
  }

  override protected def putData(resource: Resource, tx: Transaction) {

    for (version <- resource.versions) {
      val versionKey = version.systemMetadata.get(Resource.MD_DATA_ADDRESS) match {
        case Some(vk) => vk
        case None => throw new StorageException("Can't find address for data in version")
      }

      storeVersionData(versionKey, version, tx, false)
    }
  }

  def loadData(resource: Resource) = {
    for (version <- resource.versions) {

      val versionKey = version.systemMetadata.get(Resource.MD_DATA_ADDRESS) match {
        case Some(value: String) => value
        case None => throw new StorageException("Can't find data reference for version in resource: " + resource.address)
      }

      version.data = new LazyBDBDataWrapper(versionKey, getDatabase(false))
    }
  }


  override def deleteData(ra: String, tx: Transaction) {

    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    val status = getDatabase(false).get(null, key, value, LockMode.DEFAULT)

    if (status == OperationStatus.SUCCESS) {
      val resource = Resource.fromByteArray(value.getData)

      for (version <- resource.versions) {
        val dataKey = version.systemMetadata.get(Resource.MD_DATA_ADDRESS) match {
          case Some(address) => new DatabaseEntry(address.getBytes)
          case None => throw new StorageException("No data address in version for resource: " + ra)
        }

        var attempts : Int = 0
        var successFlag : Boolean = false
        try {
          do {
            attempts += 1
            masterNodeNumber = getMasterNodeNumber

            tryToWrite {
              getDatabase(true).delete(tx, dataKey)
            }

            successFlag = true
          } while(!successFlag && attempts <= 2)
        } catch {
          case e => {
            throw e
          }
        }
      }


    } else throw new ResourceNotFoundException(ra)

  }

  private def storeVersionData(key: String, version: ResourceVersion, tx: Transaction, allowOverwrite: Boolean) {

    val dbKey = new DatabaseEntry(key.getBytes)
    val dbValue = new DatabaseEntry(version.data.getBytes)

    var status: OperationStatus = null

    var attempts : Int = 0
    var successFlag : Boolean = false

    try {
      do {
        attempts += 1
        masterNodeNumber = getMasterNodeNumber

        tryToWrite {
          if (allowOverwrite) {
            status = getDatabase(true).put(tx, dbKey, dbValue)
          } else {
            status = getDatabase(true).putNoOverwrite(tx, dbKey, dbValue)
          }

          if (status != OperationStatus.SUCCESS) {
            throw new StorageException("Failed to write version data, operation status is " + status.toString)
          }
        }

        successFlag = true
      } while(!successFlag && attempts <= 2)
    } catch {
      case e => {
        throw e
      }
    }


    version.data = DataWrapper.wrap(version.data.getBytes)
    version.systemMetadata.put(Resource.MD_DATA_LENGTH, version.data.length.toString)
  }

  def name = PureBDBStorage.NAME

}

object ReplicatedBDBStorage {
  val NAME = classOf[ReplicatedBDBStorage].getSimpleName
}