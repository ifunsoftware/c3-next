package org.aphreet.c3.platform.remote.replication.impl.data.queue

import org.aphreet.c3.platform.common.Path

/**
 * Author: Mikhail Malygin
 * Date:   1/15/14
 * Time:   6:40 PM
 */
class ReplicationQueueStorageHolderImpl extends ReplicationQueueStorageHolder{

  private var replicationQueueStorage: Option[ReplicationQueueStorage] = None

  def storage: Option[ReplicationQueueStorage] = replicationQueueStorage

  def updateStoragePath(path: Option[Path]){

    path match {
      case Some(value) => replace(Some(new ReplicationQueueStorageImpl(value)))
      case None => replace(None)
    }
  }

  private def replace(queue: Option[ReplicationQueueStorage]){
    val storage = replicationQueueStorage
    replicationQueueStorage = None
    storage.map(_.close())
    replicationQueueStorage = queue
  }
}
