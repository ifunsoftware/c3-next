package org.aphreet.c3.platform

import org.aphreet.c3.platform.common.{DefaultComponentLifecycle, Logger}
import org.aphreet.c3.platform.config.EnvironmentSystemDirectoryProvider
import org.aphreet.c3.platform.config.impl.PlatformConfigComponentImpl
import org.aphreet.c3.platform.management.impl.PlatformManagementComponentImpl
import org.aphreet.c3.platform.metadata.impl.TransientMetadataComponentImpl
import org.aphreet.c3.platform.query.impl.QueryComponentImpl
import org.aphreet.c3.platform.statistics.impl.StatisticsComponentImpl
import org.aphreet.c3.platform.storage.dispatcher.impl.ZoneStorageDispatcherComponent
import org.aphreet.c3.platform.storage.dispatcher.selector.mime.MimeTypeStorageSelectorComponent
import org.aphreet.c3.platform.storage.impl.StorageComponentImpl
import org.aphreet.c3.platform.storage.migration.impl.MigrationComponentImpl
import org.aphreet.c3.platform.task.impl.TaskComponentImpl

/**
 * Author: Mikhail Malygin
 * Date:   12/9/13
 * Time:   4:47 PM
 */
object AppRunner extends App{

  val log = Logger(getClass)

  log.info("Starting c3-core")

  val app = new Object
    with DefaultComponentLifecycle
    with EnvironmentSystemDirectoryProvider
    with PlatformConfigComponentImpl
    with StatisticsComponentImpl
    with TaskComponentImpl
    with ZoneStorageDispatcherComponent
    with StorageComponentImpl
    with MigrationComponentImpl
    with MimeTypeStorageSelectorComponent
    with PlatformManagementComponentImpl
    with TransientMetadataComponentImpl
    with QueryComponentImpl

  log.info("Running initialization hooks")

  app.start()

  log.info("Startup is complete")

  Thread.sleep(10000)

  app.stop()
}
