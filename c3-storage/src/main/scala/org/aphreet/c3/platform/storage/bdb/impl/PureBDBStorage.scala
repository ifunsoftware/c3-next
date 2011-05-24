package org.aphreet.c3.platform.storage.bdb.impl

import org.aphreet.c3.platform.storage.StorageParams
import org.aphreet.c3.platform.storage.bdb._

class PureBDBStorage(override val parameters: StorageParams,
                     override val systemId:String,
                     override val config: BDBConfig)
          extends AbstractSingleInstanceBDBStorage(parameters, systemId, config)
          with BDBDataManipulator{

  def name = PureBDBStorage.NAME

}

object PureBDBStorage {
  val NAME = classOf[PureBDBStorage].getSimpleName
}
