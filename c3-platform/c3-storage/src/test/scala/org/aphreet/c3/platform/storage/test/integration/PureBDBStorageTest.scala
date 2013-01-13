package org.aphreet.c3.platform.storage.test.integration

import org.aphreet.c3.platform.storage.{RW, StorageParams, Storage}
import org.aphreet.c3.platform.storage.bdb.impl.PureBDBStorage
import collection.mutable
import org.aphreet.c3.platform.storage.bdb.BDBConfig


class PureBDBStorageTest extends AbstractStorageTestCase{

  def createStorage(id:String, params:mutable.HashMap[String, String]):Storage =
    new PureBDBStorage(
      new StorageParams(id, storagePath, "PureBDBStorage", RW(""), List(), params),
      "PBDS" + id,
      new BDBConfig(true, 20, 0, 102400))

}
