package org.aphreet.c3.platform.storage.test.integration

import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.storage.bdb.BDBConfig


import org.aphreet.c3.platform.storage.file.FileBDBStorage
import collection.mutable

class FileBDBStorageTest extends AbstractStorageTestCase{

 def createStorage(id:String):Storage =
    new FileBDBStorage(
      new StorageParams(id, storagePath, "FileBDBStorage", RW(""), List(), new mutable.HashMap[String, String]),
      "12341234",
      new BDBConfig(true, 20, 0, 102400))
}
