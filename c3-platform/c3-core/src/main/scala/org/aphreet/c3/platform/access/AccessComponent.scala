package org.aphreet.c3.platform.access

/**
 * Author: Mikhail Malygin
 * Date:   12/11/13
 * Time:   12:33 AM
 */
trait AccessComponent {

  def accessManager: AccessManager

  def accessMediator: AccessMediator

  def cleanupManager: CleanupManager
}
