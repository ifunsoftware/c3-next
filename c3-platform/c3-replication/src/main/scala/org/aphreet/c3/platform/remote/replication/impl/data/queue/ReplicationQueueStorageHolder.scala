package org.aphreet.c3.platform.remote.replication.impl.data.queue

import org.aphreet.c3.platform.common.Path

/**
 * Author: Mikhail Malygin
 * Date:   1/15/14
 * Time:   6:36 PM
 */
trait ReplicationQueueStorageHolder {

  def storage: Option[ReplicationQueueStorage]

  def updateStoragePath(path: Option[Path])

}
