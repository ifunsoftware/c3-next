package org.aphreet.c3.platform.storage.activator

import org.aphreet.c3.platform.common.{C3Activator, Logger, DefaultComponentLifecycle}
import org.aphreet.c3.platform.config.{PlatformConfigComponent, PlatformConfigManager}
import org.aphreet.c3.platform.storage.bdb.impl.PureBDBStorageComponent
import org.aphreet.c3.platform.storage.composite.CompositeStorageComponent
import org.aphreet.c3.platform.storage.file.FileBDBStorageComponent
import org.aphreet.c3.platform.storage.{StorageComponent, StorageManager}
import org.osgi.framework.{BundleContext, BundleActivator}

/**
 * Author: Mikhail Malygin
 * Date:   12/13/13
 * Time:   1:23 AM
 */
class C3StorageActivator extends C3Activator {

  var storageApp: Option[DefaultComponentLifecycle] = None

  def start(context: BundleContext) {

    log.info("Starting c3-storage")

    log.info("Resolving services")

    val storageManagerService = getService(context, classOf[StorageManager])
    val platformConfigManagerService = getService(context, classOf[PlatformConfigManager])

    trait DependencyProvider extends StorageComponent with PlatformConfigComponent{
      def storageManager: StorageManager = storageManagerService

      def platformConfigManager: PlatformConfigManager = platformConfigManagerService
    }

    log.info("Creating components")

    val app = new Object with DefaultComponentLifecycle
      with DependencyProvider
      with PureBDBStorageComponent
      with FileBDBStorageComponent
      with CompositeStorageComponent

    log.info("Running initialization callbacks")

    app.start()

    storageApp = Some(app)

    log.info("Startup is complete")
  }

  def stop(context: BundleContext) {

    log.info("Stopping components")

    storageApp.foreach(_.stop())

    log.info("c3-storage stopped")
  }

}
