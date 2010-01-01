package org.aphreet.c3.platform.storage.test.integration

import org.aphreet.c3.platform.test.integration.storage.AbstractStorageTest

import org.aphreet.c3.platform.storage.bdb.PureBDBStorage


class PureBDBStorageTest extends AbstractStorageTest{

 override def createStorage(id:String):Storage = 
    new PureBDBStorage(id, storagePath)
 
}
