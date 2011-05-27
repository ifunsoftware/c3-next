package org.aphreet.c3.platform.storage.bdb.impl

import org.aphreet.c3.platform.storage.{Storage, StorageParams}
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.storage.bdb._

/**
 * Created by IntelliJ IDEA.
 * User: antey
 * Date: 08.05.11
 * Time: 0:43
 * To change this template use File | Settings | File Templates.
 */

@Component
class ReplicatedBDBStorageFactory extends AbstractBDBStorageFactory{

  protected def createNewStorage(params:StorageParams, systemId:String):Storage = {

    val storage = new ReplicatedBDBStorage(params, systemId, bdbConfig)
    storage.ids = params.secIds
    storage
  }

  def name:String = ReplicatedBDBStorage.NAME
}