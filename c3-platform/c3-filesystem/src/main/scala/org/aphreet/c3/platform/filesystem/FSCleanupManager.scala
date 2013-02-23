package org.aphreet.c3.platform.filesystem

import lib.FSNodeBFSTreeTraversal
import org.aphreet.c3.platform.common.WatchedActor
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.filesystem.FSCleanupManagerProtocol.CleanupDirectoryTask
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.access.{ResourceDeletedMsg, AccessMediator, AccessManager}
import org.aphreet.c3.platform.storage.StorageManager
import org.aphreet.c3.platform.resource.ResourceAddress

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */
trait FSCleanupManager extends WatchedActor {}

object FSCleanupManagerProtocol {
  case class CleanupDirectoryTask(directory: Directory)
}