package org.aphreet.c3.platform.storage.composite

import com.sleepycat.je.{OperationStatus, LockMode, DatabaseEntry, Transaction}
import java.io.File
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.storage.bdb.impl.BDBDataManipulator
import org.aphreet.c3.platform.storage.bdb.{BDBConfigProvider, DatabaseProvider, DataManipulator}
import org.aphreet.c3.platform.storage.file.FileDataManipulator

trait CompositeDataManipulator extends DataManipulator with DatabaseProvider with BDBConfigProvider{

  def getDataPath:File

  def bdbDataManipulator:BDBDataManipulator = new BDBDataManipulator {

    def getDatabase(writeFlag: Boolean) =
      CompositeDataManipulator.this.getDatabase(writeFlag)

  }

  def fileDataManipulator:FileDataManipulator = new FileDataManipulator {
    def getDataPath =
      CompositeDataManipulator.this.getDataPath

    def getDatabase(writeFlag: Boolean) = {
      CompositeDataManipulator.this.getDatabase(writeFlag)
    }
  }

  override
  def loadData(resource:Resource){
    selectDataManipulator(resource).loadData(resource)
  }

  override
  def loadDataForUpdate(resource: Resource, tx: Transaction){
    selectDataManipulator(resource).loadDataForUpdate(resource, tx)
  }

  override
  def storeData(resource:Resource, tx:Transaction) {
    selectDataManipulator(resource).storeData(resource, tx)
  }

  override
  def deleteData(ra:String, tx:Transaction) {
    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    val status = rwDatabase.get(null, key, value, LockMode.DEFAULT)

    //TODO consider reuse got resource in the data manipulators implemenations
    if (status == OperationStatus.SUCCESS) {
      val resource = Resource.fromByteArray(value.getData)
      selectDataManipulator(resource).deleteData(ra, tx)
    }
  }

  protected def selectDataManipulator(resource:Resource):DataManipulator = {
    if (useFileStore(resource)){
      resource.systemMetadata.put("c3.blob.store", "file")
      fileDataManipulator
    }else{
      resource.systemMetadata.put("c3.blob.store", "bdb")
      bdbDataManipulator
    }
  }

  protected def useFileStore(resource:Resource):Boolean = {

    resource.systemMetadata.get("c3.blob.store") match {
      case Some(value) => value == "file"
      case None => (resource.versions.head.data.length > config.fileThreshold)
    }
  }
}
