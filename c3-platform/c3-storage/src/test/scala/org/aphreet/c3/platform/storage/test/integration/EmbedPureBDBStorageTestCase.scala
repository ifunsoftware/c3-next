package org.aphreet.c3.platform.storage.test.integration

import org.aphreet.c3.platform.storage.{RW, StorageParams, Storage}
import org.aphreet.c3.platform.storage.bdb.impl.PureBDBStorage
import collection.mutable
import org.aphreet.c3.platform.storage.bdb.BDBConfig

class EmbedPureBDBStorageTestCase extends AbstractStorageTestCase{

  def createStorage(id:String):Storage =
    new PureBDBStorage(
      new StorageParams(id, List(), storagePath, "PureBDBStorage", RW(""), List(), new mutable.HashMap[String, String]),
      "12341234",
      new BDBConfig(true, 20, 5120))
}