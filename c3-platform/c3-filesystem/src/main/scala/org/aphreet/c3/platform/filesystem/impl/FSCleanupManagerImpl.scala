package org.aphreet.c3.platform.filesystem.impl

import org.aphreet.c3.platform.filesystem.{Node, Directory, FSCleanupManager}
import org.aphreet.c3.platform.filesystem.lib.FSNodeBFSTreeTraversal
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.storage.StorageManager
import org.aphreet.c3.platform.access.{ResourceDeletedMsg, AccessManager, AccessMediator}
import org.aphreet.c3.platform.filesystem.FSCleanupManagerProtocol.CleanupDirectoryTask
import org.aphreet.c3.platform.resource.ResourceAddress
import org.aphreet.c3.platform.common.Logger

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */
@Component
class FSCleanupManagerImpl extends FSNodeBFSTreeTraversal with FSCleanupManager{

  val log = Logger(getClass)

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
          cleanupDirectory(dir)
        }
        case msg => {
          log.error("Unknown message is received! Msg: " + msg + ". Skipping...")
        }
      }
    }
  }

  def cleanupDirectory(d: Directory){
    val traversed = traverseFS(d).reverse

    traversed.foreach(node => removeResource(node.resource.address))
  }

  protected def resolveNodeByAddress(ra: String) = Node.fromResource(accessManager.get(ra))

  private def removeResource(ra: String){
    storageManager.storageForAddress(ResourceAddress(ra)).delete(ra)
    accessMediator ! ResourceDeletedMsg(ra, 'FSCleanupManager)
  }
}