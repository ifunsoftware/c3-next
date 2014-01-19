package org.aphreet.c3.platform

import akka.actor.ActorRefFactory
import org.aphreet.c3.platform.access.impl.AccessComponentImpl
import org.aphreet.c3.platform.access.{AccessMediator, CleanupManager, AccessManager}
import org.aphreet.c3.platform.actor.ActorComponent
import org.aphreet.c3.platform.common.{C3ActorActivator, C3AppHandle, DefaultComponentLifecycle}
import org.aphreet.c3.platform.config._
import org.aphreet.c3.platform.config.impl.PlatformConfigComponentImpl
import org.aphreet.c3.platform.config.impl.VersionComponentImpl
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.aphreet.c3.platform.management.impl.PlatformManagementComponentImpl
import org.aphreet.c3.platform.metadata.TransientMetadataManager
import org.aphreet.c3.platform.metadata.impl.TransientMetadataComponentImpl
import org.aphreet.c3.platform.query.QueryManager
import org.aphreet.c3.platform.query.impl.QueryComponentImpl
import org.aphreet.c3.platform.statistics.StatisticsManager
import org.aphreet.c3.platform.statistics.impl.StatisticsComponentImpl
import org.aphreet.c3.platform.storage.StorageManager
import org.aphreet.c3.platform.storage.dispatcher.impl.ZoneStorageDispatcherComponent
import org.aphreet.c3.platform.storage.dispatcher.selector.mime.MimeTypeStorageSelectorComponent
import org.aphreet.c3.platform.storage.impl.StorageComponentImpl
import org.aphreet.c3.platform.storage.migration.impl.MigrationComponentImpl
import org.aphreet.c3.platform.storage.updater.impl.StorageUpdaterComponentImpl
import org.aphreet.c3.platform.task.TaskManager
import org.aphreet.c3.platform.task.impl.TaskComponentImpl
import org.osgi.framework.BundleContext

/**
 * Author: Mikhail Malygin
 * Date:   12/11/13
 * Time:   1:08 AM
 */
class C3CoreActivator extends C3ActorActivator {

  def name = "c3-core"

  def createApplication(context: BundleContext, actorRefFactory: ActorRefFactory): C3AppHandle = {

    trait LocalBundleContextProvider extends BundleContextProvider {
      def bundleContext: BundleContext = context
    }

    trait DependencyProvider extends ActorComponent {
      val actorSystem = actorRefFactory
    }

    val module = new Object
      with DefaultComponentLifecycle
      with LocalBundleContextProvider
      with VersionComponentImpl
      with DependencyProvider
      with EnvironmentSystemDirectoryProvider
      with PlatformConfigComponentImpl
      with StatisticsComponentImpl
      with TaskComponentImpl
      with ZoneStorageDispatcherComponent
      with StorageComponentImpl
      with StorageUpdaterComponentImpl
      with MigrationComponentImpl
      with MimeTypeStorageSelectorComponent
      with PlatformManagementComponentImpl
      with TransientMetadataComponentImpl
      with QueryComponentImpl
      with AccessComponentImpl

    new C3AppHandle {
      def registerServices(context: BundleContext) {
        registerService(context, classOf[AccessManager], module.accessManager)
        registerService(context, classOf[AccessMediator], module.accessMediator)
        registerService(context, classOf[CleanupManager], module.cleanupManager)
        registerService(context, classOf[TaskManager], module.taskManager)
        registerService(context, classOf[StatisticsManager], module.statisticsManager)
        registerService(context, classOf[TransientMetadataManager], module.transientMetadataManager)
        registerService(context, classOf[PlatformConfigManager], module.platformConfigManager)
        registerService(context, classOf[PlatformManagementEndpoint], module.platformManagementEndpoint)
        registerService(context, classOf[QueryManager], module.queryManager)
        registerService(context, classOf[StorageManager], module.storageManager)
        registerService(context, classOf[VersionManager], module.versionManager)
        registerService(context, classOf[ConfigPersister], module.configPersister)
      }

      val app = module
    }
  }
}
