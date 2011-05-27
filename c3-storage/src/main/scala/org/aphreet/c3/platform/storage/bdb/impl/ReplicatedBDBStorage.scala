package org.aphreet.c3.platform.storage.bdb.impl

import org.aphreet.c3.platform.storage.StorageParams
import org.aphreet.c3.platform.storage.bdb._

/**
 * Created by IntelliJ IDEA.
 * User: antey
 * Date: 30.04.11
 * Time: 16:56
 * To change this template use File | Settings | File Templates.
 */

class ReplicatedBDBStorage(override val parameters: StorageParams,
                                    override val systemId:String,
                                    override val config: BDBConfig)
                    extends AbstractReplicatedBDBStorage(parameters, systemId, config)
                    with BDBDataManipulator {

  def name = ReplicatedBDBStorage.NAME

}

object ReplicatedBDBStorage {
  val NAME = classOf[ReplicatedBDBStorage].getSimpleName
}