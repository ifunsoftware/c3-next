package org.aphreet.c3.platform

import java.util.Properties
import org.aphreet.c3.platform.access.impl.AccessComponentImpl
import org.aphreet.c3.platform.access.{CleanupManager, AccessMediator, AccessManager}
import org.aphreet.c3.platform.common.{Logger, DefaultComponentLifecycle}
import org.aphreet.c3.platform.config.impl.PlatformConfigComponentImpl
import org.aphreet.c3.platform.config.impl.VersionComponentImpl
import org.aphreet.c3.platform.config.{VersionManager, BundleContextProvider, PlatformConfigManager, EnvironmentSystemDirectoryProvider}
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
import org.aphreet.c3.platform.task.TaskManager
import org.aphreet.c3.platform.task.impl.TaskComponentImpl
import org.osgi.framework.{BundleContext, BundleActivator}
import org.aphreet.c3.platform.storage.updater.impl.StorageUpdaterComponentImpl

/**
 * Author: Mikhail Malygin
 * Date:   12/11/13
 * Time:   1:08 AM
 */
class C3CoreBundleActivator extends BundleActivator {

  val log = Logger(getClass)

  var app: Option[DefaultComponentLifecycle] = None

  def start(context: BundleContext) {

    log.info("Starting c3-core")

    trait LocalBundleContextProvider extends BundleContextProvider{
      def bundleContext: BundleContext = context
    }

    val core = new Object
      with DefaultComponentLifecycle
      with LocalBundleContextProvider
      with VersionComponentImpl
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

    log.info("Running initialization hooks")

    core.start()

    registerService(context, classOf[AccessManager], core.accessManager)
    registerService(context, classOf[AccessMediator], core.accessMediator)
    registerService(context, classOf[CleanupManager], core.cleanupManager)
    registerService(context, classOf[TaskManager], core.taskManager)
    registerService(context, classOf[StatisticsManager], core.statisticsManager)
    registerService(context, classOf[TransientMetadataManager], core.transientMetadataManager)
    registerService(context, classOf[PlatformConfigManager], core.platformConfigManager)
    registerService(context, classOf[PlatformManagementEndpoint], core.platformManagementEndpoint)
    registerService(context, classOf[QueryManager], core.queryManager)
    registerService(context, classOf[StorageManager], core.storageManager)
    registerService(context, classOf[VersionManager], core.versionManager)

    log.info("Startup is complete")

    app = Some(core)

  }

  protected def registerService[T](context: BundleContext, clazz: Class[T], service: T){
    context.registerService(clazz.getCanonicalName, service, new Properties())
  }

  def stop(context: BundleContext) {

    log.info("Stopping c3-core")

    app.foreach(_.stop())

    log.info("c3-core is stopped")

    context.ungetService()
  }
}
