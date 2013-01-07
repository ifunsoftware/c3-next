package org.aphreet.c3.platform.storage.test.integration

import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.storage.bdb.BDBConfig


import org.aphreet.c3.platform.storage.file.FileBDBStorage
import collection.mutable

class FileBDBStorageTest extends AbstractStorageTestCase{

 def createStorage(id:String, params:mutable.HashMap[String, String]):Storage =
    new FileBDBStorage(
      new StorageParams(id, storagePath, "FileBDBStorage", RW(""), List(), params),
      "12341234",
      new BDBConfig(true, 20, 0, 102400))
}
