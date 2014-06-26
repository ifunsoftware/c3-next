package org.aphreet.c3.platform.backup

import org.aphreet.c3.platform.access.{AccessManager, AccessMediator, AccessComponent}
import org.aphreet.c3.platform.backup.impl.BackupComponentImpl
import org.aphreet.c3.platform.common.{C3SimpleActivator, C3AppHandle, DefaultComponentLifecycle}
import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigPersister, PlatformConfigComponent}
import org.aphreet.c3.platform.filesystem.{FSManager, FSComponent}
import org.aphreet.c3.platform.storage.{StorageManager, StorageComponent}
import org.aphreet.c3.platform.task.{TaskManager, TaskComponent}
import org.osgi.framework.BundleContext
import org.aphreet.c3.platform.query.{QueryManager, QueryComponent}

/**
 * Author: Mikhail Malygin
 * Date:   12/21/13
 * Time:   3:36 PM
 */
class C3BackupActivator extends C3SimpleActivator {

  def name = "c3-backup"

  def createApplication(context: BundleContext): C3AppHandle = {

    trait DependencyProvider extends AccessComponent
    with StorageComponent
    with PlatformConfigComponent
    with TaskComponent
    with QueryComponent
    with FSComponent {
      val accessManager = getService(context, classOf[AccessManager])

      val accessMediator = getService(context, classOf[AccessMediator])

      val storageManager = getService(context, classOf[StorageManager])

      val taskManager = getService(context, classOf[TaskManager])

      val platformConfigManager = getService(context, classOf[PlatformConfigManager])

      val configPersister = getService(context, classOf[ConfigPersister])

      val filesystemManager: FSManager = getService(context, classOf[FSManager])

      val queryManager = getService(context, classOf[QueryManager])
    }

    log.info("Starting c3-backup")

    val module = new Object with DefaultComponentLifecycle
      with DependencyProvider
      with BackupComponentImpl

    new C3AppHandle {
      def registerServices(context: BundleContext) {
        registerService(context, classOf[BackupManager], module.backupManager)
      }

      val app = module
    }
  }
}
