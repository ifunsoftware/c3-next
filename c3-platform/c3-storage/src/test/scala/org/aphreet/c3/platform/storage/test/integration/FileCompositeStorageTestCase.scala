package org.aphreet.c3.platform.storage.test.integration

import org.aphreet.c3.platform.storage.{RW, StorageParams, Storage}
import org.aphreet.c3.platform.storage.composite.CompositeStorage
import collection.mutable
import org.aphreet.c3.platform.storage.bdb.BDBConfig

class FileCompositeStorageTestCase extends AbstractStorageTestCase{

  def createStorage(id:String):Storage =
        new CompositeStorage(
          new StorageParams(id, storagePath, "CompositeStorage", RW(""), List(), new mutable.HashMap[String, String]),
          "12341234",
          new BDBConfig(true, 20, 0, 0))
}
