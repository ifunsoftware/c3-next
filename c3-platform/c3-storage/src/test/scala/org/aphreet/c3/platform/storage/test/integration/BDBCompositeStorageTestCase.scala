package org.aphreet.c3.platform.storage.test.integration

import org.aphreet.c3.platform.storage.{RW, StorageParams, Storage}
import collection.mutable
import org.aphreet.c3.platform.storage.bdb.BDBConfig
import org.aphreet.c3.platform.storage.composite.CompositeStorage


class BDBCompositeStorageTestCase extends AbstractStorageTestCase{

  def createStorage(id:String, params:mutable.HashMap[String, String]):Storage =
      new CompositeStorage(
        new StorageParams(id, storagePath, "CompositeStorage", RW(""), List(), params),
        "BDCS" + id,
        new BDBConfig(true, 20, 0, 102400), conflictResolverProvider)
}
