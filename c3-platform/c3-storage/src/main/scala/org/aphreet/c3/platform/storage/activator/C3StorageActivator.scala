package org.aphreet.c3.platform.storage.activator

import org.aphreet.c3.platform.common.{C3AppHandle, C3Activator, Logger, DefaultComponentLifecycle}
import org.aphreet.c3.platform.config.{ConfigPersister, PlatformConfigComponent, PlatformConfigManager}
import org.aphreet.c3.platform.storage.bdb.impl.PureBDBStorageComponent
import org.aphreet.c3.platform.storage.composite.CompositeStorageComponent
import org.aphreet.c3.platform.storage.file.FileBDBStorageComponent
import org.aphreet.c3.platform.storage.{StorageComponent, StorageManager}
import org.osgi.framework.{BundleContext, BundleActivator}
import org.aphreet.c3.platform.actor.ActorComponent
import akka.actor.ActorSystem

/**
 * Author: Mikhail Malygin
 * Date:   12/13/13
 * Time:   1:23 AM
 */
class C3StorageActivator extends C3Activator {

  def name = "c3-storage"

  def createApplication(context: BundleContext): C3AppHandle = {
    trait DependencyProvider extends StorageComponent with PlatformConfigComponent with ActorComponent{
      val storageManager = getService(context, classOf[StorageManager])

      val platformConfigManager = getService(context, classOf[PlatformConfigManager])

      val configPersister = getService(context, classOf[ConfigPersister])

      val actorSystem = getService(context, classOf[ActorSystem])
    }

    val module = new Object with DefaultComponentLifecycle
      with DependencyProvider
      with PureBDBStorageComponent
      with FileBDBStorageComponent
      with CompositeStorageComponent

    new C3AppHandle {
      def registerServices(context: BundleContext) {}

      val app = module
    }

  }
}
