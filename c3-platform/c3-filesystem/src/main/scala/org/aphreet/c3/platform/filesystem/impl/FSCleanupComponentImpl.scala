package org.aphreet.c3.platform.filesystem.impl

import org.aphreet.c3.platform.access.{AccessComponent, ResourceDeletedMsg}
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.common.{ComponentLifecycle, Logger}
import org.aphreet.c3.platform.filesystem.FSCleanupManagerProtocol.CleanupDirectoryTask
import org.aphreet.c3.platform.filesystem.lib.FSNodeBFSTreeTraversal
import org.aphreet.c3.platform.filesystem.{FSCleanupComponent, Node, Directory, FSCleanupManager}
import org.aphreet.c3.platform.resource.ResourceAddress
import org.aphreet.c3.platform.storage.StorageComponent
import org.aphreet.c3.platform.actor.ActorComponent
import akka.actor.{Props, Actor, ActorRefFactory}

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */

trait FSCleanupComponentImpl extends FSCleanupComponent {

  this: ComponentLifecycle
    with StorageComponent
    with AccessComponent
    with ActorComponent =>

  private val fsCleanupManagerImpl = new FSCleanupManagerImpl(actorSystem)

  def filesystemCleanupManager: FSCleanupManager = fsCleanupManagerImpl

  class FSCleanupManagerImpl(val actorSystem: ActorRefFactory) extends FSNodeBFSTreeTraversal with FSCleanupManager{

    val log = Logger(getClass)

    val async = actorSystem.actorOf(Props.create(classOf[FSCleanupManagerActor], this))

    {
      log.info("Starting FS Cleanup manager...")
    }

    class FSCleanupManagerActor extends Actor{
      def receive = {
        case task: CleanupDirectoryTask => {
          cleanupDirectory(task.directory)
        }
      }
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