package org.aphreet.c3.platform.filesystem.impl

import org.aphreet.c3.platform.access.{AccessComponent, ResourceDeletedMsg}
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.common.{ComponentLifecycle, Logger}
import org.aphreet.c3.platform.filesystem.FSCleanupManagerProtocol.CleanupDirectoryTask
import org.aphreet.c3.platform.filesystem.lib.FSNodeBFSTreeTraversal
import org.aphreet.c3.platform.filesystem.{FSCleanupComponent, Node, Directory, FSCleanupManager}
import org.aphreet.c3.platform.resource.ResourceAddress
import org.aphreet.c3.platform.storage.StorageComponent

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */

trait FSCleanupComponentImpl extends FSCleanupComponent {

  this: ComponentLifecycle
    with StorageComponent
    with AccessComponent =>

  private val fsCleanupManagerImpl = new FSCleanupManagerImpl

  destroy(Unit => fsCleanupManagerImpl.destroy())

  def filesystemCleanupManager: FSCleanupManager = fsCleanupManagerImpl

  class FSCleanupManagerImpl extends FSNodeBFSTreeTraversal with FSCleanupManager{

    val log = Logger(getClass)

    {
      log.info("Starting FS Cleanup manager...")
      start()
    }

    override def act() {
      loop{
        react{
          case task: CleanupDirectoryTask => {
            val dir = task.directory
            cleanupDirectory(dir)
          }
          case DestroyMsg => {
            this.exit()
          }
          case msg => {
            log.error("Unknown message is received! Msg: " + msg + ". Skipping...")
          }
        }
      }
    }

    def destroy(){
      log.info("Stopping FSCleanupManager")
      this ! DestroyMsg
    }

    def cleanupDirectory(d: Directory){
      val traversed = traverseFS(d).reverse

      traversed.foreach(node => removeResource(node.resource.address))
    }

    protected def resolveNodeByAddress(ra: String) = Node.fromResource(accessManager.get(ra))

    private def removeResource(ra: String){
      storageManager.storageForAddress(ResourceAddress(ra))
        .map(_.delete(ra))
        .map(accessMediator ! ResourceDeletedMsg(_, 'FSCleanupManager))
    }
  }
}