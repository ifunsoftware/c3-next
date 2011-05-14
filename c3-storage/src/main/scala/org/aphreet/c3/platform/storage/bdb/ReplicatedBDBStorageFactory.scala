package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.storage.common.AbstractBDBStorageFactory
import org.aphreet.c3.platform.storage.{Storage, StorageParams}

/**
 * Created by IntelliJ IDEA.
 * User: antey
 * Date: 08.05.11
 * Time: 0:43
 * To change this template use File | Settings | File Templates.
 */

class ReplicatedBDBStorageFactory extends AbstractBDBStorageFactory{

  protected def createNewStorage(params:StorageParams, systemId:String):Storage = {

    val storage = new ReplicatedBDBStorage(params, systemId, bdbConfig)
    storage.ids = params.secIds
    storage
  }

  def name:String = ReplicatedBDBStorage.NAME
}