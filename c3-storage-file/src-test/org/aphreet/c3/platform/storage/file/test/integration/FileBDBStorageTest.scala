package org.aphreet.c3.platform.storage.file.test.integration

import org.aphreet.c3.platform.test.integration.storage.AbstractStorageTest

import org.aphreet.c3.platform.storage.file.FileBDBStorage

class FileBDBStorageTest extends AbstractStorageTest{

  override def createStorage(id:String):Storage = 
    new FileBDBStorage(id, storagePath)
  
  
}
