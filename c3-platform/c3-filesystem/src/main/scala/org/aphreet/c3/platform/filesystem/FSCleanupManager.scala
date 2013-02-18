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
@Component
class FSCleanupManager extends FSNodeBFSTreeTraversal with WatchedActor {

  val log = LogFactory getLog getClass

  @Autowired
  var storageManager: StorageManager = _

  @Autowired
  var accessMediator: AccessMediator = _

  @Autowired
  var accessManager: AccessManager = _

  {
    log.info("Starting FS Cleanup manager...")
  }

  override def act() {
    loop{
      react{
        case task: CleanupDirectoryTask => {
          val dir = task.directory
          val traversed = traverseFS(dir).reverse

          traversed.foreach(node => removeResource(node.resource.address))
        }
        case msg => {
          log.error("Unknown message is received! Msg: " + msg + ". Skipping...")
        }
      }
    }
  }

  protected def resolveNodeByAddress(ra: String) = Node.fromResource(accessManager.get(ra))

  private def removeResource(ra: String){
    storageManager.storageForAddress(ResourceAddress(ra)).delete(ra)
    accessMediator ! ResourceDeletedMsg(ra, 'FSCleanupManager)
  }
}

object FSCleanupManagerProtocol {
  case class CleanupDirectoryTask(directory: Directory)
}