package org.aphreet.c3.platform.filesystem

import org.aphreet.c3.platform.common.WatchedActor

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */
trait FSCleanupManager extends WatchedActor {

  def cleanupDirectory(d: Directory)

}

trait FSCleanupComponent {

  def filesystemCleanupManager: FSCleanupManager

}

object FSCleanupManagerProtocol {
  case class CleanupDirectoryTask(directory: Directory)
}