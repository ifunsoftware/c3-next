package org.aphreet.c3.platform.storage.activator

import akka.actor.ActorRefFactory
import org.aphreet.c3.platform.actor.ActorComponent
import org.aphreet.c3.platform.common._
import org.aphreet.c3.platform.config.{ConfigPersister, PlatformConfigComponent, PlatformConfigManager}
import org.aphreet.c3.platform.storage.bdb.impl.PureBDBStorageComponent
import org.aphreet.c3.platform.storage.composite.CompositeStorageComponent
import org.aphreet.c3.platform.storage.file.FileBDBStorageComponent
import org.aphreet.c3.platform.storage.{StorageComponent, StorageManager}
import org.osgi.framework.BundleContext

/**
 * Author: Mikhail Malygin
 * Date:   12/13/13
 * Time:   1:23 AM
 */
class C3StorageActivator extends C3ActorActivator {

  def name = "c3-storage"

  def createApplication(context: BundleContext, actorRefFactory: ActorRefFactory): C3AppHandle = {
    trait DependencyProvider extends StorageComponent with PlatformConfigComponent with ActorComponent {
      val storageManager = getService(context, classOf[StorageManager])

      val platformConfigManager = getService(context, classOf[PlatformConfigManager])

      val configPersister = getService(context, classOf[ConfigPersister])

      val actorSystem = actorRefFactory
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
